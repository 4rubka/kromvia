
use std::collections::HashMap;
use std::io::{Read, Write};
use std::path::Path;
use std::sync::Arc;

use serde::{Deserialize, Serialize};

use super::modrinth::{latest_version, primary_file};
use super::ContentKind;
use crate::engine::core::http::{self, Download, Hash};
use crate::engine::core::jobs::Job;
use crate::engine::core::paths::{self, safe_join};
use crate::engine::game::install::{download_all, FileTask};
use crate::engine::game::loaders::{Loader, LoaderKind};
use crate::engine::instances::{self, Instance};

const ALLOWED_HOSTS: [&str; 4] = ["cdn.modrinth.com", "github.com", "raw.githubusercontent.com", "gitlab.com"];

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct Index {
    #[serde(default)]
    format_version: u32,
    #[serde(default)]
    game: String,
    #[serde(default)]
    version_id: String,
    name: String,
    files: Vec<PackFile>,
    dependencies: HashMap<String, String>,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct PackFile {
    path: String,
    hashes: PackHashes,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    env: Option<PackEnv>,
    downloads: Vec<String>,
    file_size: u64,
}

#[derive(Serialize, Deserialize)]
struct PackHashes {
    #[serde(default, skip_serializing_if = "String::is_empty")]
    sha1: String,
    sha512: String,
}

#[derive(Serialize, Deserialize)]
struct PackEnv {
    client: String,
    #[serde(default)]
    server: String,
}

fn pack_loader(deps: &HashMap<String, String>, mc: &str) -> Option<Loader> {
    let (key, kind) = [
        ("fabric-loader", LoaderKind::Fabric),
        ("quilt-loader", LoaderKind::Quilt),
        ("neoforge", LoaderKind::NeoForge),
        ("forge", LoaderKind::Forge),
    ]
    .into_iter()
    .find(|(key, _)| deps.contains_key(*key))?;
    let v = &deps[key];
    let version = if kind == LoaderKind::Forge && !v.starts_with(&format!("{mc}-")) { format!("{mc}-{v}") } else { v.clone() };
    Some(Loader { kind, version })
}

fn allowed_url(url: &str) -> bool {
    ALLOWED_HOSTS.iter().any(|h| url.starts_with(&format!("https://{h}/")))
}

pub async fn install_from_modrinth(project_id: &str, job: &Arc<Job>) -> Result<Instance, String> {
    job.progress("resolve", 0, None);
    let version = latest_version(project_id, ContentKind::Modpack, None).await?;
    let file = primary_file(&version)?;
    if file.filename.contains(['/', '\\']) || !file.filename.ends_with(".mrpack") {
        return Err(format!("not a modpack file: {}", file.filename));
    }
    let archive = paths::temp().join(&file.filename);
    let d = Download { url: &file.url, dest: &archive, hash: Some(Hash::Sha512(file.hashes.sha512.clone())), size: Some(file.size) };
    http::download(&d, job.cancel_flag(), |done, total| job.progress("download", done, total)).await?;
    let result = install_archive(&archive, job).await;
    let _ = std::fs::remove_file(&archive);
    result
}

pub async fn install_from_file(archive: &Path, job: &Arc<Job>) -> Result<Instance, String> {
    if archive.extension().is_none_or(|e| e != "mrpack") || !archive.is_file() {
        return Err("pick a .mrpack file".into());
    }
    install_archive(archive, job).await
}

pub async fn install_manifest(manifest: serde_json::Value, job: &Arc<Job>) -> Result<Instance, String> {
    let index: Index = serde_json::from_value(manifest).map_err(|e| format!("bad build manifest: {e}"))?;
    let mc = index.dependencies.get("minecraft").ok_or("the build does not say which Minecraft it needs")?;
    let inst = instances::create(&index.name, mc, pack_loader(&index.dependencies, mc))?;
    let result = fill_instance(&inst, &index, None, job).await;
    if result.is_err() {
        let _ = instances::delete(&inst.id);
    }
    result.map(|()| inst)
}

async fn install_archive(archive: &Path, job: &Arc<Job>) -> Result<Instance, String> {
    let index: Index = {
        let mut zip = zip::ZipArchive::new(std::fs::File::open(archive).map_err(|e| e.to_string())?).map_err(|e| e.to_string())?;
        let mut json = String::new();
        zip.by_name("modrinth.index.json")
            .map_err(|_| "modrinth.index.json missing")?
            .read_to_string(&mut json)
            .map_err(|e| e.to_string())?;
        serde_json::from_str(&json).map_err(|e| format!("bad modrinth.index.json: {e}"))?
    };

    let mc = index.dependencies.get("minecraft").ok_or("modpack does not say which Minecraft it needs")?;
    let inst = instances::create(&index.name, mc, pack_loader(&index.dependencies, mc))?;

    let result = fill_instance(&inst, &index, Some(archive), job).await;
    if result.is_err() {
        let _ = instances::delete(&inst.id);
    }
    result.map(|()| inst)
}

async fn fill_instance(inst: &Instance, index: &Index, archive: Option<&Path>, job: &Arc<Job>) -> Result<(), String> {
    let game = instances::game_dir(&inst.id)?;
    let mut tasks = Vec::new();
    for f in &index.files {
        if f.env.as_ref().is_some_and(|e| e.client == "unsupported") {
            continue;
        }
        let url = f.downloads.iter().find(|u| allowed_url(u)).ok_or_else(|| format!("{}: no allowed download URL", f.path))?;
        tasks.push(FileTask {
            url: url.clone(),
            dest: safe_join(&game, &f.path)?,
            hash: Some(Hash::Sha512(f.hashes.sha512.clone())),
            size: Some(f.file_size),
        });
    }
    download_all(tasks, job).await?;

    let Some(archive) = archive else { return Ok(()) };
    job.progress("extract", 0, None);
    let (archive, game) = (archive.to_path_buf(), game.clone());
    tokio::task::spawn_blocking(move || extract_overrides(&archive, &game, &["overrides/", "client-overrides/"]))
        .await
        .map_err(|e| e.to_string())?
}

pub(super) fn extract_overrides(archive: &Path, game: &Path, prefixes: &[&str]) -> Result<(), String> {
    let mut zip = zip::ZipArchive::new(std::fs::File::open(archive).map_err(|e| e.to_string())?).map_err(|e| e.to_string())?;
    for &prefix in prefixes {
        for i in 0..zip.len() {
            let mut entry = zip.by_index(i).map_err(|e| e.to_string())?;
            let name = entry.name().map_err(|e| e.to_string())?.to_string();
            let Some(rel) = name.strip_prefix(prefix).map(str::to_string) else { continue };
            if rel.is_empty() || entry.is_dir() {
                continue;
            }
            let dest = safe_join(game, &rel)?;
            std::fs::create_dir_all(dest.parent().unwrap()).map_err(|e| e.to_string())?;
            let mut out = std::fs::File::create(&dest).map_err(|e| e.to_string())?;
            std::io::copy(&mut entry, &mut out).map_err(|e| e.to_string())?;
        }
    }
    Ok(())
}

fn loader_dependency(loader: &Loader, mc: &str) -> Result<(&'static str, String), String> {
    Ok(match loader.kind {
        LoaderKind::Fabric => ("fabric-loader", loader.version.clone()),
        LoaderKind::Quilt => ("quilt-loader", loader.version.clone()),
        LoaderKind::NeoForge => ("neoforge", loader.version.clone()),
        LoaderKind::Forge => ("forge", loader.version.strip_prefix(&format!("{mc}-")).unwrap_or(&loader.version).to_string()),
        LoaderKind::LegacyFabric => return Err("the .mrpack format has no Legacy Fabric".into()),
        LoaderKind::Ornithe => return Err("the .mrpack format has no Ornithe".into()),
    })
}

#[derive(Deserialize)]
struct LookupVersion {
    files: Vec<LookupFile>,
}

#[derive(Deserialize)]
struct LookupFile {
    url: String,
    hashes: HashMap<String, String>,
}

async fn lookup_urls(sha1s: &[String]) -> Result<HashMap<String, String>, String> {
    if sha1s.is_empty() {
        return Ok(HashMap::new());
    }
    let url = "https://api.modrinth.com/v2/version_files";
    let body = serde_json::json!({ "hashes": sha1s, "algorithm": "sha1" });
    let resp = http::client().post(url).json(&body).send().await.map_err(|e| format!("{url}: {e}"))?;
    if !resp.status().is_success() {
        return Err(format!("{url}: HTTP {}", resp.status()));
    }
    let found: HashMap<String, LookupVersion> = resp.json().await.map_err(|e| format!("{url}: {e}"))?;
    Ok(found
        .into_iter()
        .filter_map(|(sha1, v)| {
            let f = v.files.into_iter().find(|f| f.hashes.get("sha1") == Some(&sha1))?;
            allowed_url(&f.url).then_some((sha1, f.url))
        })
        .collect())
}

pub async fn export(instance_id: &str, dest: &Path, job: &Arc<Job>) -> Result<(), String> {
    if dest.extension().is_none_or(|e| e != "mrpack") {
        return Err("export file must end in .mrpack".into());
    }
    let (index, overrides) = build_index(instance_id, job).await?;
    let game = instances::game_dir(instance_id)?;
    job.progress("pack", 0, None);
    let (dest, config) = (dest.to_path_buf(), game.join("config"));
    tokio::task::spawn_blocking(move || write_pack(&dest, &index, &overrides, &config))
        .await
        .map_err(|e| e.to_string())?
}

pub async fn manifest(instance_id: &str, job: &Arc<Job>) -> Result<serde_json::Value, String> {
    let (index, _) = build_index(instance_id, job).await?;
    serde_json::to_value(index).map_err(|e| e.to_string())
}

async fn build_index(instance_id: &str, job: &Arc<Job>) -> Result<(Index, Vec<(String, std::path::PathBuf)>), String> {
    let inst = instances::get(instance_id)?;
    let mut dependencies = HashMap::from([("minecraft".to_string(), inst.mc_version.clone())]);
    if let Some(loader) = &inst.loader {
        let (key, v) = loader_dependency(loader, &inst.mc_version)?;
        dependencies.insert(key.into(), v);
    }

    job.progress("resolve", 0, None);
    let game = instances::game_dir(instance_id)?;
    let content: Vec<(String, std::path::PathBuf)> = super::list(instance_id)?
        .into_iter()
        .filter(|f| f.enabled)
        .map(|f| (format!("{}/{}", f.folder, f.file), game.join(&f.folder).join(&f.file)))
        .collect();
    let hashed = {
        let content = content.clone();
        tokio::task::spawn_blocking(move || {
            content
                .iter()
                .map(|(_, p)| Ok((http::sha1_file(p)?, http::sha512_file(p)?, std::fs::metadata(p).map_err(|e| e.to_string())?.len())))
                .collect::<Result<Vec<_>, String>>()
        })
        .await
        .map_err(|e| e.to_string())??
    };
    let urls = lookup_urls(&hashed.iter().map(|h| h.0.clone()).collect::<Vec<_>>()).await?;

    let mut files = Vec::new();
    let mut overrides = Vec::new();
    for ((rel, path), (sha1, sha512, size)) in content.into_iter().zip(hashed) {
        match urls.get(&sha1) {
            Some(url) => files.push(PackFile {
                path: rel,
                hashes: PackHashes { sha1, sha512 },
                env: None,
                downloads: vec![url.clone()],
                file_size: size,
            }),
            None => overrides.push((rel, path)),
        }
    }
    let index = Index {
        format_version: 1,
        game: "minecraft".into(),
        version_id: "1.0.0".into(),
        name: inst.name.clone(),
        files,
        dependencies,
    };
    Ok((index, overrides))
}

fn write_pack(dest: &Path, index: &Index, overrides: &[(String, std::path::PathBuf)], config: &Path) -> Result<(), String> {
    let part = dest.with_extension("mrpack.part");
    let result = (|| {
        let mut zip = zip::ZipWriter::new(std::fs::File::create(&part).map_err(|e| e.to_string())?);
        let opts = zip::write::SimpleFileOptions::default();
        zip.start_file("modrinth.index.json", opts).map_err(|e| e.to_string())?;
        zip.write_all(&serde_json::to_vec_pretty(index).map_err(|e| e.to_string())?).map_err(|e| e.to_string())?;
        let mut add = |rel: &str, path: &Path| -> Result<(), String> {
            zip.start_file(format!("overrides/{rel}"), opts).map_err(|e| e.to_string())?;
            std::io::copy(&mut std::fs::File::open(path).map_err(|e| e.to_string())?, &mut zip).map_err(|e| e.to_string())?;
            Ok(())
        };
        for (rel, path) in overrides {
            add(rel, path)?;
        }
        for (rel, path) in walk(config, "config") {
            add(&rel, &path)?;
        }
        zip.finish().map_err(|e| e.to_string())?;
        Ok(())
    })();
    match result {
        Ok(()) => std::fs::rename(&part, dest).map_err(|e| e.to_string()),
        Err(e) => {
            let _ = std::fs::remove_file(&part);
            Err(e)
        }
    }
}

pub fn walk(dir: &Path, prefix: &str) -> Vec<(String, std::path::PathBuf)> {
    let mut out = Vec::new();
    for entry in std::fs::read_dir(dir).into_iter().flatten().flatten() {
        let (Ok(kind), Some(name)) = (entry.file_type(), entry.file_name().to_str().map(String::from)) else { continue };
        let rel = format!("{prefix}/{name}");
        if kind.is_dir() {
            out.extend(walk(&entry.path(), &rel));
        } else if kind.is_file() {
            out.push((rel, entry.path()));
        }
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn maps_pack_loaders() {
        let deps = HashMap::from([("minecraft".to_string(), "1.20.1".to_string()), ("forge".to_string(), "47.4.0".to_string())]);
        assert_eq!(pack_loader(&deps, "1.20.1"), Some(Loader { kind: LoaderKind::Forge, version: "1.20.1-47.4.0".into() }));
        let deps = HashMap::from([("quilt-loader".to_string(), "0.29.0".to_string())]);
        assert_eq!(pack_loader(&deps, "1.21.1").unwrap().kind, LoaderKind::Quilt);
        assert_eq!(pack_loader(&HashMap::new(), "1.21.1"), None);
    }

    #[test]
    fn loader_dependency_round_trips() {
        let forge = Loader { kind: LoaderKind::Forge, version: "1.20.1-47.4.0".into() };
        let (key, v) = loader_dependency(&forge, "1.20.1").unwrap();
        assert_eq!((key, v.as_str()), ("forge", "47.4.0"));
        let deps = HashMap::from([(key.to_string(), v)]);
        assert_eq!(pack_loader(&deps, "1.20.1"), Some(forge));
        assert!(loader_dependency(&Loader { kind: LoaderKind::LegacyFabric, version: "0.16".into() }, "1.8.9").is_err());
    }

    #[test]
    fn writes_readable_pack() {
        let dir = std::env::temp_dir().join(format!("kromvia-mrpack-{}", std::process::id()));
        std::fs::create_dir_all(dir.join("config/sub")).unwrap();
        std::fs::write(dir.join("config/sub/a.toml"), "x=1").unwrap();
        std::fs::write(dir.join("local.jar"), "jar").unwrap();
        let index = Index {
            format_version: 1,
            game: "minecraft".into(),
            version_id: "1.0.0".into(),
            name: "T".into(),
            files: vec![],
            dependencies: HashMap::from([("minecraft".into(), "1.21.4".into())]),
        };
        let dest = dir.join("t.mrpack");
        write_pack(&dest, &index, &[("mods/local.jar".into(), dir.join("local.jar"))], &dir.join("config")).unwrap();
        let mut zip = zip::ZipArchive::new(std::fs::File::open(&dest).unwrap()).unwrap();
        let mut names: Vec<String> = zip.file_names().map(|n| n.unwrap().into_owned()).collect();
        names.sort();
        assert_eq!(names, ["modrinth.index.json", "overrides/config/sub/a.toml", "overrides/mods/local.jar"]);
        let mut json = String::new();
        zip.by_name("modrinth.index.json").unwrap().read_to_string(&mut json).unwrap();
        let back: Index = serde_json::from_str(&json).unwrap();
        assert_eq!((back.format_version, back.name.as_str()), (1, "T"));
        std::fs::remove_dir_all(dir).unwrap();
    }

    #[test]
    fn only_spec_hosts_are_allowed() {
        assert!(allowed_url("https://cdn.modrinth.com/data/x/y.jar"));
        assert!(allowed_url("https://github.com/a/b/releases/download/c.jar"));
        assert!(!allowed_url("http://cdn.modrinth.com/data/x.jar"));
        assert!(!allowed_url("https://cdn.modrinth.com.evil.io/x.jar"));
        assert!(!allowed_url("https://evil.io/x.jar"));
    }
}
