
use std::collections::HashMap;
use std::io::{Read, Write};
use std::path::Path;
use std::sync::Arc;

use serde::{Deserialize, Serialize};

use super::modrinth::{self, latest_version, primary_file, Version};
use super::packref::{self, PackPath, PackRef};
use super::ContentKind;
use crate::engine::core::http::{self, Download, Hash};
use crate::engine::core::jobs::Job;
use crate::engine::core::paths::{self, safe_join};
use crate::engine::game::install::{download_all, FileTask};
use crate::engine::game::loaders::{Loader, LoaderKind};
use crate::engine::instances::{self, Instance};

const ALLOWED_HOSTS: [&str; 6] = ["cdn.modrinth.com", "github.com", "raw.githubusercontent.com", "gitlab.com", "edge.forgecdn.net", "mediafilez.forgecdn.net"];

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
    /// Shared builds only: the modpack to install first; `files` then hold what was added on top.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pack: Option<PackRef>,
    /// Shared builds only: files of that pack the player took out.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    removed: Vec<String>,
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
    install_modrinth_version(&version, job).await
}

async fn install_modrinth_version(version: &Version, job: &Arc<Job>) -> Result<Instance, String> {
    let file = primary_file(version)?;
    if file.filename.contains(['/', '\\']) || !file.filename.ends_with(".mrpack") {
        return Err(format!("not a modpack file: {}", file.filename));
    }
    let archive = paths::temp().join(&file.filename);
    let d = Download { url: &file.url, dest: &archive, hash: Some(Hash::Sha512(file.hashes.sha512.clone())), size: Some(file.size) };
    http::download(&d, job.cancel_flag(), |done, total| job.progress("download", done, total)).await?;
    let result = install_archive(&archive, job).await;
    let _ = std::fs::remove_file(&archive);
    let (inst, pack_files) = result?;
    let pack = PackRef { source: "modrinth".into(), project: version.project_id.clone(), file: version.id.clone() };
    packref::save(&inst.id, pack, pack_files)?;
    Ok(inst)
}

pub async fn install_from_file(archive: &Path, job: &Arc<Job>) -> Result<Instance, String> {
    if archive.extension().is_none_or(|e| e != "mrpack") || !archive.is_file() {
        return Err("pick a .mrpack file".into());
    }
    install_archive(archive, job).await.map(|(inst, _)| inst)
}

pub async fn install_manifest(manifest: serde_json::Value, job: &Arc<Job>) -> Result<Instance, String> {
    let index: Index = serde_json::from_value(manifest).map_err(|e| format!("bad build manifest: {e}"))?;
    if let Some(pack) = &index.pack {
        return install_on_pack(pack, &index, job).await;
    }
    let mc = index.dependencies.get("minecraft").ok_or("the build does not say which Minecraft it needs")?;
    let inst = instances::create(&index.name, mc, pack_loader(&index.dependencies, mc))?;
    let result = fill_instance(&inst, &index, None, job).await;
    if result.is_err() {
        let _ = instances::delete(&inst.id);
    }
    result.map(|()| inst)
}

async fn install_on_pack(pack: &PackRef, index: &Index, job: &Arc<Job>) -> Result<Instance, String> {
    let (inst, blocked) = match pack.source.as_str() {
        "curseforge" => {
            let id = |s: &str| s.parse::<u64>().map_err(|_| format!("bad CurseForge id {s}"));
            super::curseforge::install_pack_version(id(&pack.project)?, id(&pack.file)?, job).await?
        }
        "modrinth" => {
            let version = modrinth::version(&pack.file).await?;
            if version.project_id != pack.project {
                return Err(format!("Modrinth version {} is not part of project {}", pack.file, pack.project));
            }
            (install_modrinth_version(&version, job).await?, vec![])
        }
        other => return Err(format!("unknown modpack source {other}")),
    };
    let game = instances::game_dir(&inst.id)?;
    for rel in &index.removed {
        let path = safe_join(&game, rel)?;
        if path.is_file() {
            std::fs::remove_file(&path).map_err(|e| e.to_string())?;
        }
    }
    fill_instance(&inst, index, None, job).await?;
    if !blocked.is_empty() {
        return Err(format!(
            "the build is installed, but these authors only allow downloads from the CurseForge website; add them to the mods folder by hand: {}",
            blocked.join(", ")
        ));
    }
    Ok(inst)
}

async fn install_archive(archive: &Path, job: &Arc<Job>) -> Result<(Instance, Vec<PackPath>), String> {
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
    let pack_files = index.files.iter().filter(|f| !f.hashes.sha1.is_empty()).map(|f| PackPath { path: f.path.clone(), sha1: f.hashes.sha1.clone() }).collect();
    result.map(|()| (inst, pack_files))
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
    let (index, overrides) = build_index(instance_id, job, false).await?;
    let game = instances::game_dir(instance_id)?;
    job.progress("pack", 0, None);
    let (dest, config) = (dest.to_path_buf(), game.join("config"));
    tokio::task::spawn_blocking(move || write_pack(&dest, &index, &overrides, &config))
        .await
        .map_err(|e| e.to_string())?
}

/// A build to share by link, and the names of the files that can't travel with it: the link
/// carries only download URLs, and nothing on Modrinth or CurseForge matches those files.
pub async fn manifest(instance_id: &str, job: &Arc<Job>) -> Result<(serde_json::Value, Vec<String>), String> {
    let (index, left) = build_index(instance_id, job, true).await?;
    let missing = left.into_iter().map(|(rel, _)| rel).collect();
    Ok((serde_json::to_value(index).map_err(|e| e.to_string())?, missing))
}

/// Lists the instance's enabled content with download URLs from Modrinth. For a shared link
/// (`share`) it also asks CurseForge, and when the instance came from a modpack it points at the
/// pack and keeps only what differs from it. Whatever has no URL comes back as overrides.
async fn build_index(instance_id: &str, job: &Arc<Job>, share: bool) -> Result<(Index, Vec<(String, std::path::PathBuf)>), String> {
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

    let base = if share { packref::load(instance_id) } else { None };
    let (mut content, mut hashed) = (content, hashed);
    let mut removed = Vec::new();
    if let Some(base) = &base {
        let in_pack: std::collections::HashSet<&str> = base.files.iter().map(|f| f.sha1.as_str()).collect();
        let keep: Vec<bool> = hashed.iter().map(|h| !in_pack.contains(h.0.as_str())).collect();
        let mut it = keep.iter();
        content.retain(|_| *it.next().unwrap());
        let mut it = keep.iter();
        hashed.retain(|_| *it.next().unwrap());
        removed = base.files.iter().filter(|f| !game.join(&f.path).is_file()).map(|f| f.path.clone()).collect();
    }

    let mut urls = lookup_urls(&hashed.iter().map(|h| h.0.clone()).collect::<Vec<_>>()).await?;
    if share {
        let unresolved: Vec<(String, std::path::PathBuf)> =
            content.iter().zip(&hashed).filter(|(_, h)| !urls.contains_key(&h.0)).map(|((_, p), h)| (h.0.clone(), p.clone())).collect();
        let prints = tokio::task::spawn_blocking(move || {
            unresolved
                .into_iter()
                .map(|(sha1, p)| Ok((super::curseforge::fingerprint(&std::fs::read(&p).map_err(|e| e.to_string())?), sha1)))
                .collect::<Result<HashMap<u32, String>, String>>()
        })
        .await
        .map_err(|e| e.to_string())??;
        let found = super::curseforge::urls_by_fingerprint(&prints.keys().copied().collect::<Vec<_>>()).await?;
        for (print, url) in found {
            if let Some(sha1) = prints.get(&print) {
                if allowed_url(&url) {
                    urls.insert(sha1.clone(), url);
                }
            }
        }
    }

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
        pack: base.map(|b| b.pack),
        removed,
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
            pack: None,
            removed: vec![],
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
