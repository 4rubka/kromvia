
use std::collections::HashMap;
use std::io::Read;
use std::path::{Path, PathBuf};
use std::process::Stdio;
use std::sync::Arc;

use serde::Deserialize;
use tokio::process::Command;

use super::install::{client_jar, download_all, FileTask};
use super::loaders::{installer_url, Loader};
use super::version::{maven_path, Library, VersionJson};
use crate::engine::core::http::{self, Download, Hash};
use crate::engine::core::jobs::Job;
use crate::engine::core::paths::{self, safe_join};

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct InstallProfile {
    #[serde(default)]
    json: Option<String>,
    #[serde(default)]
    data: HashMap<String, Sided>,
    #[serde(default)]
    processors: Vec<Processor>,
    #[serde(default)]
    libraries: Vec<Library>,
    #[serde(default)]
    install: Option<LegacyInstall>,
    #[serde(default)]
    version_info: Option<VersionJson>,
}

#[derive(Deserialize)]
struct Sided {
    client: String,
}

#[derive(Deserialize)]
struct Processor {
    #[serde(default)]
    sides: Option<Vec<String>>,
    jar: String,
    #[serde(default)]
    classpath: Vec<String>,
    #[serde(default)]
    args: Vec<String>,
    #[serde(default)]
    outputs: HashMap<String, String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct LegacyInstall {
    path: String,
    file_path: String,
}

type Zip = zip::ZipArchive<std::fs::File>;

fn read_entry(zip: &mut Zip, name: &str) -> Result<Vec<u8>, String> {
    let mut entry = zip.by_name(name).map_err(|_| format!("installer has no {name}"))?;
    let mut buf = Vec::new();
    entry.read_to_end(&mut buf).map_err(|e| e.to_string())?;
    Ok(buf)
}

fn extract_entry(zip: &mut Zip, name: &str, dest: &Path) -> Result<(), String> {
    let bytes = read_entry(zip, name)?;
    std::fs::create_dir_all(dest.parent().unwrap()).map_err(|e| e.to_string())?;
    std::fs::write(dest, bytes).map_err(|e| e.to_string())
}

fn library_file(coord: &str) -> Result<PathBuf, String> {
    safe_join(&paths::libraries(), &maven_path(coord).ok_or_else(|| format!("bad maven coordinate {coord}"))?)
}

pub async fn install(loader: &Loader, mc: &str, java: &Path, job: &Arc<Job>) -> Result<VersionJson, String> {
    let url = installer_url(loader)?;
    let sha1 = http::client()
        .get(format!("{url}.sha1"))
        .send()
        .await
        .and_then(|r| r.error_for_status())
        .map_err(|e| format!("{url}: {e}"))?
        .text()
        .await
        .map_err(|e| e.to_string())?;
    let installer = paths::temp().join(format!("{}-installer.jar", loader.version));
    let d = Download { url: &url, dest: &installer, hash: Some(Hash::Sha1(sha1.trim().to_string())), size: None };
    http::download(&d, job.cancel_flag(), |done, total| job.progress("download", done, total)).await?;

    let result = run_installer(&installer, mc, java, job).await;
    let _ = std::fs::remove_file(&installer);
    result
}

async fn run_installer(installer: &Path, mc: &str, java: &Path, job: &Arc<Job>) -> Result<VersionJson, String> {
    let mut zip = zip::ZipArchive::new(std::fs::File::open(installer).map_err(|e| e.to_string())?).map_err(|e| e.to_string())?;
    let profile: InstallProfile = serde_json::from_slice(&read_entry(&mut zip, "install_profile.json")?)
        .map_err(|e| format!("bad install_profile.json: {e}"))?;

    if let (Some(info), Some(install)) = (profile.version_info, profile.install) {
        extract_entry(&mut zip, &install.file_path, &library_file(&install.path)?)?;
        return Ok(info);
    }

    let json_name = profile.json.as_deref().unwrap_or("/version.json").trim_start_matches('/').to_string();
    let version: VersionJson =
        serde_json::from_slice(&read_entry(&mut zip, &json_name)?).map_err(|e| format!("bad {json_name}: {e}"))?;

    let mut tasks = Vec::new();
    for lib in profile.libraries.iter().chain(&version.libraries) {
        let Some(art) = lib.artifact() else { continue };
        let path = art.path.ok_or_else(|| format!("library {} has no path", lib.name))?;
        let dest = safe_join(&paths::libraries(), &path)?;
        if art.url.is_empty() {
            extract_entry(&mut zip, &format!("maven/{path}"), &dest)?;
        } else {
            tasks.push(FileTask { url: art.url, dest, hash: art.sha1.map(Hash::Sha1), size: art.size });
        }
    }
    download_all(tasks, job).await?;

    let processors: Vec<&Processor> = profile
        .processors
        .iter()
        .filter(|p| p.sides.as_ref().is_none_or(|s| s.iter().any(|x| x == "client")))
        .collect();
    if processors.is_empty() {
        return Ok(version);
    }

    let work = paths::temp().join(format!("forge-{}", version.id));
    let _ = std::fs::remove_dir_all(&work);
    let mut vars: HashMap<String, String> = HashMap::from([
        ("SIDE".into(), "client".into()),
        ("MINECRAFT_JAR".into(), client_jar(mc)?.to_string_lossy().into_owned()),
        ("MINECRAFT_VERSION".into(), mc.to_string()),
        ("ROOT".into(), paths::root().to_string_lossy().into_owned()),
        ("INSTALLER".into(), installer.to_string_lossy().into_owned()),
        ("LIBRARY_DIR".into(), paths::libraries().to_string_lossy().into_owned()),
    ]);
    for (key, value) in &profile.data {
        let v = &value.client;
        let resolved = if let Some(coord) = v.strip_prefix('[').and_then(|s| s.strip_suffix(']')) {
            library_file(coord)?.to_string_lossy().into_owned()
        } else if let Some(lit) = v.strip_prefix('\'').and_then(|s| s.strip_suffix('\'')) {
            lit.to_string()
        } else if let Some(entry) = v.strip_prefix('/') {
            let dest = safe_join(&work, entry)?;
            extract_entry(&mut zip, entry, &dest)?;
            dest.to_string_lossy().into_owned()
        } else {
            v.clone()
        };
        vars.insert(key.clone(), resolved);
    }
    drop(zip);

    let total = processors.len() as u64;
    for (i, proc) in processors.into_iter().enumerate() {
        job.progress("patch", i as u64, Some(total));
        if job.is_cancelled() {
            return Err("cancelled".into());
        }
        run_processor(proc, &vars, java).await?;
    }
    job.progress("patch", total, Some(total));
    let _ = std::fs::remove_dir_all(&work);
    Ok(version)
}

fn resolve_arg(arg: &str, vars: &HashMap<String, String>) -> Result<String, String> {
    if let Some(key) = arg.strip_prefix('{').and_then(|s| s.strip_suffix('}')) {
        return vars.get(key).cloned().ok_or_else(|| format!("installer variable {key} is not defined"));
    }
    if let Some(coord) = arg.strip_prefix('[').and_then(|s| s.strip_suffix(']')) {
        return Ok(library_file(coord)?.to_string_lossy().into_owned());
    }
    Ok(arg.to_string())
}

fn outputs_ok(outputs: &[(String, String)]) -> bool {
    !outputs.is_empty()
        && outputs.iter().all(|(path, sha1)| {
            crate::engine::core::http::sha1_file(Path::new(path)).is_ok_and(|h| h.eq_ignore_ascii_case(sha1))
        })
}

async fn run_processor(proc: &Processor, vars: &HashMap<String, String>, java: &Path) -> Result<(), String> {
    let outputs: Vec<(String, String)> = proc
        .outputs
        .iter()
        .map(|(k, v)| Ok((resolve_arg(k, vars)?, resolve_arg(v, vars)?)))
        .collect::<Result<_, String>>()?;
    if outputs_ok(&outputs) {
        return Ok(());
    }

    let jar = library_file(&proc.jar)?;
    let main = main_class(&jar)?;
    let mut cp = vec![jar];
    for c in &proc.classpath {
        cp.push(library_file(c)?);
    }
    let sep = if cfg!(windows) { ";" } else { ":" };
    let cp = cp.iter().map(|p| p.to_string_lossy()).collect::<Vec<_>>().join(sep);
    let args = proc.args.iter().map(|a| resolve_arg(a, vars)).collect::<Result<Vec<_>, _>>()?;

    let mut cmd = Command::new(java);
    cmd.arg("-cp").arg(cp).arg(&main).args(&args).stdin(Stdio::null()).stdout(Stdio::piped()).stderr(Stdio::piped());
    #[cfg(windows)]
    cmd.creation_flags(0x0800_0000); // CREATE_NO_WINDOW
    let out = cmd.output().await.map_err(|e| format!("{main}: {e}"))?;
    if !out.status.success() {
        let log = String::from_utf8_lossy(&out.stderr);
        let tail: Vec<&str> = log.lines().rev().take(15).collect();
        return Err(format!("{main} failed: {}", tail.into_iter().rev().collect::<Vec<_>>().join("\n")));
    }
    for (path, want) in &outputs {
        let got = crate::engine::core::http::sha1_file(Path::new(path))?;
        if !got.eq_ignore_ascii_case(want) {
            return Err(format!("{main} produced a bad {path}"));
        }
    }
    Ok(())
}

fn main_class(jar: &Path) -> Result<String, String> {
    let mut zip = zip::ZipArchive::new(std::fs::File::open(jar).map_err(|e| e.to_string())?).map_err(|e| e.to_string())?;
    let manifest = String::from_utf8_lossy(&read_entry(&mut zip, "META-INF/MANIFEST.MF")?).replace("\r\n", "\n").replace("\n ", "");
    manifest
        .lines()
        .find_map(|l| l.strip_prefix("Main-Class:"))
        .map(|s| s.trim().to_string())
        .ok_or_else(|| format!("{} has no Main-Class", jar.display()))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn resolves_processor_args() {
        let vars = HashMap::from([("SIDE".to_string(), "client".to_string())]);
        assert_eq!(resolve_arg("{SIDE}", &vars).unwrap(), "client");
        assert_eq!(resolve_arg("--task", &vars).unwrap(), "--task");
        assert!(resolve_arg("{NOPE}", &vars).is_err());
    }
}
