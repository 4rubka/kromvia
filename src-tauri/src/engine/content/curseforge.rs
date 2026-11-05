
use std::collections::{HashMap, HashSet, VecDeque};
use std::io::Read;
use std::path::Path;
use std::sync::Arc;

use serde::Deserialize;

use super::modrinth::{Category, SearchFilters, SearchHit, SearchResult};
use super::packref::{self, PackPath, PackRef};
use super::{read_index, write_index, ContentEntry, ContentKind};
use crate::engine::core::http::{self, Download, Hash};
use crate::engine::core::jobs::Job;
use crate::engine::core::paths::{self, safe_join};
use crate::engine::game::install::{download_all, FileTask};
use crate::engine::game::loaders::LoaderKind;
use crate::engine::import::curseforge_loader;
use crate::engine::instances::{self, Instance};
use crate::engine::social;

const MINECRAFT: &str = "432";
const SOURCE: &str = "curseforge";

#[derive(Deserialize)]
struct Data<T> {
    data: T,
}

#[derive(Deserialize)]
struct Paged<T> {
    data: T,
    pagination: Pagination,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct Pagination {
    total_count: u64,
}

#[derive(Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
struct Mod {
    id: u64,
    name: String,
    #[serde(default)]
    slug: String,
    #[serde(default)]
    summary: String,
    #[serde(default)]
    download_count: f64,
    logo: Option<Logo>,
    #[serde(default)]
    authors: Vec<Author>,
    class_id: Option<u32>,
    #[serde(default)]
    thumbs_up_count: u64,
    #[serde(default)]
    date_modified: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct CfCategory {
    id: u64,
    name: String,
    #[serde(default)]
    is_class: bool,
}

#[derive(Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
struct Logo {
    thumbnail_url: String,
}

#[derive(Deserialize, Clone)]
struct Author {
    name: String,
}

#[derive(Deserialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct File {
    pub id: u64,
    pub mod_id: u64,
    #[serde(default)]
    pub display_name: String,
    pub file_name: String,
    release_type: u8,
    file_length: u64,
    pub download_url: Option<String>,
    #[serde(default)]
    pub file_fingerprint: u32,
    #[serde(default)]
    hashes: Vec<FileHash>,
    #[serde(default)]
    dependencies: Vec<FileDependency>,
}

#[derive(Deserialize, Clone, Debug)]
struct FileHash {
    value: String,
    algo: u8,
}

#[derive(Deserialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
struct FileDependency {
    mod_id: u64,
    relation_type: u8,
}

const REQUIRED: u8 = 3;

fn api() -> String {
    format!("{}/cf/v1", social::base())
}

async fn get<T: serde::de::DeserializeOwned>(path: &str, query: &[(&str, &str)]) -> Result<T, String> {
    http::get_json_query(&format!("{}{path}", api()), query).await.map_err(no_key)
}

async fn post<T: serde::de::DeserializeOwned>(path: &str, body: serde_json::Value) -> Result<T, String> {
    let url = format!("{}{path}", api());
    let resp = http::client().post(&url).json(&body).send().await.map_err(|e| format!("{url}: {e}"))?;
    if !resp.status().is_success() {
        return Err(no_key(format!("{url}: HTTP {}", resp.status())));
    }
    resp.json().await.map_err(|e| format!("{url}: {e}"))
}

fn no_key(e: String) -> String {
    if e.contains("HTTP 503") { "CurseForge is not available yet".into() } else { e }
}

fn class_id(kind: ContentKind) -> Result<&'static str, String> {
    Ok(match kind {
        ContentKind::Mod => "6",
        ContentKind::Modpack => "4471",
        ContentKind::ResourcePack => "12",
        ContentKind::Shader => "6552",
        ContentKind::Datapack => return Err("datapacks come from Modrinth only".into()),
    })
}

fn folder_of_class(class: Option<u32>) -> &'static str {
    match class {
        Some(12) => "resourcepacks",
        Some(6552) => "shaderpacks",
        _ => "mods",
    }
}

fn loader_type(kind: LoaderKind) -> Result<&'static str, String> {
    match kind {
        LoaderKind::Forge => Ok("1"),
        LoaderKind::Fabric | LoaderKind::Quilt => Ok("4"),
        LoaderKind::NeoForge => Ok("6"),
        LoaderKind::LegacyFabric => Err("CurseForge has no Legacy Fabric mods".into()),
        LoaderKind::Ornithe => Err("CurseForge has no Ornithe mods".into()),
    }
}

fn filters(kind: ContentKind, inst: Option<&Instance>) -> Result<Vec<(&'static str, String)>, String> {
    let mut q = vec![];
    if let (Some(inst), true) = (inst, kind != ContentKind::Modpack) {
        q.push(("gameVersion", inst.mc_version.clone()));
        if kind == ContentKind::Mod {
            if let Some(l) = &inst.loader {
                q.push(("modLoaderType", loader_type(l.kind)?.to_string()));
            }
        }
    }
    Ok(q)
}

pub async fn search(query: &str, kind: ContentKind, inst: Option<&Instance>, offset: u32, f: &SearchFilters) -> Result<SearchResult, String> {
    let offset = offset.min(9_980).to_string();
    let sort = match f.sort.as_deref() {
        Some("downloads") => "6",
        Some("follows") => "12",
        Some("newest") => "11",
        Some("updated") => "3",
        _ => "2",
    };
    let mut q: Vec<(&str, String)> = vec![
        ("gameId", MINECRAFT.into()),
        ("classId", class_id(kind)?.into()),
        ("searchFilter", query.into()),
        ("sortField", sort.into()),
        ("sortOrder", "desc".into()),
        ("pageSize", "20".into()),
        ("index", offset),
    ];
    q.extend(filters(kind, inst)?);
    if inst.is_none() || kind == ContentKind::Modpack {
        if let Some(v) = &f.game_version {
            q.push(("gameVersion", v.clone()));
        }
        let loader = match f.loader.as_deref() {
            Some("forge") => Some("1"),
            Some("fabric" | "quilt") => Some("4"),
            Some("neoforge") => Some("6"),
            _ => None,
        };
        if let Some(l) = loader {
            q.push(("modLoaderType", l.into()));
        }
    }
    if !f.categories.is_empty() {
        q.push(("categoryIds", format!("[{}]", f.categories.join(","))));
    }
    let q: Vec<(&str, &str)> = q.iter().map(|(k, v)| (*k, v.as_str())).collect();
    let page: Paged<Vec<Mod>> = get("/mods/search", &q).await?;
    Ok(SearchResult {
        total_hits: page.pagination.total_count.min(10_000),
        hits: page
            .data
            .into_iter()
            .map(|m| SearchHit {
                project_id: m.id.to_string(),
                slug: m.slug,
                title: m.name,
                description: m.summary,
                icon_url: m.logo.map(|l| l.thumbnail_url),
                downloads: m.download_count as u64,
                author: m.authors.into_iter().next().map(|a| a.name).unwrap_or_default(),
                follows: m.thumbs_up_count,
                date_modified: m.date_modified,
            })
            .collect(),
    })
}

pub async fn categories(kind: ContentKind) -> Result<Vec<Category>, String> {
    let class = class_id(kind)?;
    let list: Data<Vec<CfCategory>> = get("/categories", &[("gameId", MINECRAFT), ("classId", class)]).await?;
    let mut out: Vec<Category> =
        list.data.into_iter().filter(|c| !c.is_class).map(|c| Category { id: c.id.to_string(), name: c.name }).collect();
    out.sort_by(|a, b| a.name.cmp(&b.name));
    Ok(out)
}

fn mod_id(id: &str) -> Result<u64, String> {
    id.parse().map_err(|_| format!("bad CurseForge id {id}"))
}

pub async fn latest_file(mod_id: u64, kind: ContentKind, inst: Option<&Instance>) -> Result<File, String> {
    let mut q = filters(kind, inst)?;
    q.push(("pageSize", "50".into()));
    let q: Vec<(&str, &str)> = q.iter().map(|(k, v)| (*k, v.as_str())).collect();
    let mut files: Vec<File> = get::<Data<Vec<File>>>(&format!("/mods/{mod_id}/files"), &q).await?.data;
    files.sort_by_key(|f| std::cmp::Reverse(f.id));
    files.iter().find(|f| f.release_type == 1).or(files.first()).cloned().ok_or_else(|| "no compatible version".into())
}

async fn mods(ids: &[u64]) -> Result<HashMap<u64, Mod>, String> {
    if ids.is_empty() {
        return Ok(HashMap::new());
    }
    let list: Data<Vec<Mod>> = post("/mods", serde_json::json!({ "modIds": ids })).await?;
    Ok(list.data.into_iter().map(|m| (m.id, m)).collect())
}

async fn files(ids: &[u64]) -> Result<Vec<File>, String> {
    if ids.is_empty() {
        return Ok(vec![]);
    }
    Ok(post::<Data<Vec<File>>>("/mods/files", serde_json::json!({ "fileIds": ids })).await?.data)
}

fn sha1(f: &File) -> Option<Hash> {
    f.hashes.iter().find(|h| h.algo == 1).map(|h| Hash::Sha1(h.value.clone()))
}

fn check_name(f: &File) -> Result<(), String> {
    let n = &f.file_name;
    if n.is_empty() || n == "." || n == ".." || n.contains(['/', '\\', ':']) || n.chars().any(char::is_control) {
        return Err(format!("bad file name {}", f.file_name));
    }
    Ok(())
}

fn url(f: &File, name: &str) -> Result<String, String> {
    f.download_url.clone().ok_or_else(|| format!("{name}: the author only allows downloads from the CurseForge website"))
}

pub fn task(f: &File, dest: std::path::PathBuf, name: &str) -> Result<FileTask, String> {
    check_name(f)?;
    Ok(FileTask { url: url(f, name)?, dest, hash: sha1(f), size: Some(f.file_length) })
}

pub async fn install(instance_id: &str, project_id: &str, kind: ContentKind, job: &Arc<Job>) -> Result<Vec<String>, String> {
    let inst = instances::get(instance_id)?;
    let folder = kind.folder().ok_or("modpacks are installed as new instances")?;
    if kind == ContentKind::Mod && inst.loader.is_none() {
        return Err("mods need an instance with a mod loader".into());
    }
    let game = instances::game_dir(instance_id)?;
    let mut index = read_index(instance_id);
    let mut seen: HashSet<u64> = index.iter().filter(|e| e.is_curseforge()).filter_map(|e| e.project_id.parse().ok()).collect();
    let mut queue = VecDeque::from([mod_id(project_id)?]);
    let mut planned: Vec<File> = vec![];

    job.progress("resolve", 0, None);
    while let Some(id) = queue.pop_front() {
        if !seen.insert(id) {
            continue;
        }
        let f = latest_file(id, kind, Some(&inst)).await.map_err(|e| format!("{id}: {e}"))?;
        if kind == ContentKind::Mod {
            queue.extend(f.dependencies.iter().filter(|d| d.relation_type == REQUIRED && !seen.contains(&d.mod_id)).map(|d| d.mod_id));
        }
        planned.push(f);
    }
    let names = mods(&planned.iter().map(|f| f.mod_id).collect::<Vec<_>>()).await?;
    let title = |f: &File| names.get(&f.mod_id).map(|m| m.name.clone()).unwrap_or_else(|| f.display_name.clone());

    let tasks = planned
        .iter()
        .map(|f| task(f, safe_join(&game.join(folder), &f.file_name)?, &title(f)))
        .collect::<Result<Vec<_>, String>>()?;
    download_all(tasks, job).await?;

    let mut added = vec![];
    for f in &planned {
        index.retain(|e| !(e.folder == folder && e.file == f.file_name));
        index.push(entry(f, title(f), folder));
        added.push(title(f));
    }
    write_index(instance_id, &index)?;
    Ok(added)
}

pub fn entry(f: &File, title: String, folder: &str) -> ContentEntry {
    ContentEntry {
        project_id: f.mod_id.to_string(),
        version_id: f.id.to_string(),
        title,
        folder: folder.into(),
        file: f.file_name.clone(),
        source: Some(SOURCE.into()),
    }
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct Manifest {
    minecraft: ManifestGame,
    name: String,
    #[serde(default)]
    files: Vec<ManifestFile>,
    #[serde(default = "default_overrides")]
    overrides: String,
}

fn default_overrides() -> String {
    "overrides".into()
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ManifestGame {
    version: String,
    #[serde(default)]
    mod_loaders: Vec<ManifestLoader>,
}

#[derive(Deserialize)]
struct ManifestLoader {
    id: String,
    #[serde(default)]
    primary: bool,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ManifestFile {
    #[serde(rename = "projectID")]
    project_id: u64,
    #[serde(rename = "fileID")]
    file_id: u64,
    #[serde(default = "yes")]
    required: bool,
}

fn yes() -> bool {
    true
}

fn read_manifest(archive: &Path) -> Result<Manifest, String> {
    let mut zip = zip::ZipArchive::new(std::fs::File::open(archive).map_err(|e| e.to_string())?).map_err(|e| e.to_string())?;
    let mut json = String::new();
    zip.by_name("manifest.json").map_err(|_| "manifest.json missing: not a CurseForge modpack")?.read_to_string(&mut json).map_err(|e| e.to_string())?;
    let m: Manifest = serde_json::from_str(&json).map_err(|e| format!("bad manifest.json: {e}"))?;
    if m.overrides.is_empty() || m.overrides.contains(['/', '\\']) || m.overrides.contains("..") {
        return Err(format!("bad overrides folder {}", m.overrides));
    }
    Ok(m)
}

pub async fn install_pack(project_id: &str, job: &Arc<Job>) -> Result<Instance, String> {
    job.progress("resolve", 0, None);
    let f = latest_file(mod_id(project_id)?, ContentKind::Modpack, None).await?;
    let (inst, blocked) = install_pack_file(&f, job).await?;
    blocked_error(inst, blocked)
}

/// Installs one exact file of a pack, and leaves the mods CurseForge refuses to hand out to the caller.
pub async fn install_pack_version(project_id: u64, file_id: u64, job: &Arc<Job>) -> Result<(Instance, Vec<String>), String> {
    job.progress("resolve", 0, None);
    let f = files(&[file_id]).await?.into_iter().next().ok_or_else(|| format!("CurseForge file {file_id} not found"))?;
    if f.mod_id != project_id {
        return Err(format!("CurseForge file {file_id} is not part of project {project_id}"));
    }
    install_pack_file(&f, job).await
}

async fn install_pack_file(f: &File, job: &Arc<Job>) -> Result<(Instance, Vec<String>), String> {
    check_name(f)?;
    let archive = paths::temp().join(format!("cf-pack-{}.zip", f.id));
    let url = url(f, &f.display_name)?;
    let d = Download { url: &url, dest: &archive, hash: sha1(f), size: Some(f.file_length) };
    http::download(&d, job.cancel_flag(), |done, total| job.progress("download", done, total)).await?;
    let result = install_archive(&archive, job).await;
    let _ = std::fs::remove_file(&archive);
    let (inst, blocked, pack_files) = result?;
    let pack = PackRef { source: SOURCE.into(), project: f.mod_id.to_string(), file: f.id.to_string() };
    packref::save(&inst.id, pack, pack_files)?;
    Ok((inst, blocked))
}

pub async fn install_zip(archive: &Path, job: &Arc<Job>) -> Result<Instance, String> {
    let (inst, blocked, _) = install_archive(archive, job).await?;
    blocked_error(inst, blocked)
}

fn blocked_error(inst: Instance, blocked: Vec<String>) -> Result<Instance, String> {
    if blocked.is_empty() {
        return Ok(inst);
    }
    Err(format!(
        "the pack is installed, but these authors only allow downloads from the CurseForge website; add them to the mods folder by hand: {}",
        blocked.join(", ")
    ))
}

async fn install_archive(archive: &Path, job: &Arc<Job>) -> Result<(Instance, Vec<String>, Vec<PackPath>), String> {
    let m = read_manifest(archive)?;
    let mc = &m.minecraft.version;
    let loader = m.minecraft.mod_loaders.iter().find(|l| l.primary).or(m.minecraft.mod_loaders.first());
    let loader = match loader {
        Some(l) => Some(curseforge_loader(&l.id, mc).ok_or_else(|| format!("unsupported mod loader {}", l.id))?),
        None => None,
    };
    let inst = instances::create(&m.name, mc, loader)?;
    match fill(&inst, &m, archive, job).await {
        Ok((blocked, pack_files)) => Ok((inst, blocked, pack_files)),
        Err(e) => {
            let _ = instances::delete(&inst.id);
            Err(e)
        }
    }
}

async fn fill(inst: &Instance, m: &Manifest, archive: &Path, job: &Arc<Job>) -> Result<(Vec<String>, Vec<PackPath>), String> {
    let game = instances::game_dir(&inst.id)?;
    job.progress("resolve", 0, None);
    let wanted: Vec<&ManifestFile> = m.files.iter().filter(|f| f.required).collect();
    let list = files(&wanted.iter().map(|f| f.file_id).collect::<Vec<_>>()).await?;
    let info = mods(&wanted.iter().map(|f| f.project_id).collect::<Vec<_>>()).await?;

    let (mut tasks, mut blocked, mut entries, mut pack_files) = (vec![], vec![], vec![], vec![]);
    for f in &list {
        let m = info.get(&f.mod_id);
        let name = m.map(|m| m.name.clone()).unwrap_or_else(|| f.display_name.clone());
        let folder = folder_of_class(m.and_then(|m| m.class_id));
        if let Some(sha1) = sha1_hex(f) {
            pack_files.push(PackPath { path: format!("{folder}/{}", f.file_name), sha1 });
        }
        if f.download_url.is_none() {
            blocked.push(name);
            continue;
        }
        tasks.push(task(f, safe_join(&game.join(folder), &f.file_name)?, &name)?);
        entries.push(entry(f, name, folder));
    }
    download_all(tasks, job).await?;

    job.progress("extract", 0, None);
    let (archive, dest, prefix) = (archive.to_path_buf(), game.clone(), format!("{}/", m.overrides));
    tokio::task::spawn_blocking(move || super::mrpack::extract_overrides(&archive, &dest, &[&prefix]))
        .await
        .map_err(|e| e.to_string())??;
    write_index(&inst.id, &entries)?;
    Ok((blocked, pack_files))
}

fn sha1_hex(f: &File) -> Option<String> {
    f.hashes.iter().find(|h| h.algo == 1).map(|h| h.value.to_lowercase())
}

/// CurseForge's file fingerprint: MurmurHash2 (seed 1) over the bytes with whitespace removed.
pub fn fingerprint(bytes: &[u8]) -> u32 {
    const M: u32 = 0x5bd1_e995;
    let data: Vec<u8> = bytes.iter().copied().filter(|b| !matches!(b, 9 | 10 | 13 | 32)).collect();
    let mut h: u32 = 1 ^ data.len() as u32;
    let (chunks, rest) = data.as_chunks::<4>();
    for c in chunks {
        let mut k = u32::from_le_bytes(*c);
        k = k.wrapping_mul(M);
        k ^= k >> 24;
        k = k.wrapping_mul(M);
        h = h.wrapping_mul(M) ^ k;
    }
    if !rest.is_empty() {
        for (i, b) in rest.iter().enumerate() {
            h ^= (*b as u32) << (8 * i);
        }
        h = h.wrapping_mul(M);
    }
    h ^= h >> 13;
    h = h.wrapping_mul(M);
    h ^ (h >> 15)
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct FingerprintMatches {
    #[serde(default)]
    exact_matches: Vec<FingerprintMatch>,
}

#[derive(Deserialize)]
struct FingerprintMatch {
    file: File,
}

/// Download URLs of files CurseForge knows by fingerprint. Files whose authors block third-party
/// downloads are left out.
pub async fn urls_by_fingerprint(fingerprints: &[u32]) -> Result<HashMap<u32, String>, String> {
    if fingerprints.is_empty() {
        return Ok(HashMap::new());
    }
    let found: Data<FingerprintMatches> = post(&format!("/fingerprints/{MINECRAFT}"), serde_json::json!({ "fingerprints": fingerprints })).await?;
    Ok(found.data.exact_matches.into_iter().filter_map(|m| Some((m.file.file_fingerprint, m.file.download_url?))).collect())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn fingerprint_skips_whitespace() {
        assert_eq!(fingerprint(b""), 1540447798);
        assert_eq!(fingerprint(b"a"), 626045324);
        assert_eq!(fingerprint(b"Hello, World!\n"), 1961219979);
        assert_eq!(fingerprint(b"abc def\tghi\r\n"), 326929598);
    }
    use std::io::Write;

    #[test]
    fn reads_pack_manifest() {
        let dir = std::env::temp_dir().join(format!("kromvia-cfpack-{}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        let path = dir.join("p.zip");
        let mut zip = zip::ZipWriter::new(std::fs::File::create(&path).unwrap());
        zip.start_file("manifest.json", zip::write::SimpleFileOptions::default()).unwrap();
        zip.write_all(
            br#"{"minecraft":{"version":"1.20.1","modLoaders":[{"id":"forge-47.2.0","primary":true}]},"manifestType":"minecraftModpack",
                "name":"Pack","files":[{"projectID":238222,"fileID":4593548,"required":true}],"overrides":"overrides"}"#,
        )
        .unwrap();
        zip.finish().unwrap();
        let m = read_manifest(&path).unwrap();
        assert_eq!((m.name.as_str(), m.minecraft.version.as_str(), m.files[0].file_id), ("Pack", "1.20.1", 4593548));
        assert_eq!(curseforge_loader(&m.minecraft.mod_loaders[0].id, "1.20.1").unwrap().version, "1.20.1-47.2.0");
        std::fs::remove_dir_all(dir).unwrap();
    }

    #[test]
    fn parses_files() {
        let f: File = serde_json::from_str(
            r#"{"id":5,"modId":7,"displayName":"JEI","fileName":"jei.jar","releaseType":1,"fileLength":10,"downloadUrl":null,
                "hashes":[{"value":"md5","algo":2},{"value":"abc","algo":1}],"dependencies":[{"modId":9,"relationType":3}]}"#,
        )
        .unwrap();
        assert!(matches!(sha1(&f), Some(Hash::Sha1(h)) if h == "abc"));
        assert!(url(&f, "JEI").is_err());
        assert_eq!(f.dependencies[0].relation_type, REQUIRED);
    }
}
