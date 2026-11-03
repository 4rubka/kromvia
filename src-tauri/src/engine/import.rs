
use std::path::{Path, PathBuf};
use std::sync::Arc;

use serde::{Deserialize, Serialize};

use crate::engine::core::jobs::Job;
use crate::engine::game::loaders::{Loader, LoaderKind};
use crate::engine::instances::{self, Instance};

#[derive(Serialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct Found {
    pub source: &'static str,
    pub path: String,
    pub name: String,
    pub mc_version: String,
    pub loader: Option<Loader>,
    #[serde(skip)]
    game: Option<PathBuf>,
    #[serde(skip)]
    only: Option<&'static [&'static str]>,
}

#[derive(Deserialize)]
struct Pack {
    components: Vec<Component>,
}

#[derive(Deserialize)]
struct Component {
    uid: String,
    version: Option<String>,
}

const SKIP: &[&str] = &["logs", "crash-reports", ".cache"];

const VANILLA_ONLY: &[&str] = &[
    "mods", "config", "resourcepacks", "shaderpacks", "saves", "screenshots", "schematics",
    "options.txt", "optionsof.txt", "optionsshaders.txt", "servers.dat",
];

fn home() -> PathBuf {
    std::env::var_os("HOME").or_else(|| std::env::var_os("USERPROFILE")).map(PathBuf::from).unwrap_or_default()
}

fn prism_roots() -> Vec<PathBuf> {
    let home = home();
    let mut out = vec![];
    if cfg!(target_os = "windows") {
        if let Some(appdata) = std::env::var_os("APPDATA") {
            out.push(PathBuf::from(appdata).join("PrismLauncher"));
        }
    } else if cfg!(target_os = "macos") {
        out.push(home.join("Library/Application Support/PrismLauncher"));
    } else {
        let data = std::env::var_os("XDG_DATA_HOME").map(PathBuf::from).unwrap_or_else(|| home.join(".local/share"));
        out.push(data.join("PrismLauncher"));
        out.push(home.join(".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher"));
        out.push(data.join("multimc"));
    }
    out
}

fn loader_of(c: &Component) -> Option<Loader> {
    let kind = match c.uid.as_str() {
        "net.fabricmc.fabric-loader" => LoaderKind::Fabric,
        "org.quiltmc.quilt-loader" => LoaderKind::Quilt,
        "net.minecraftforge" => LoaderKind::Forge,
        "net.neoforged" => LoaderKind::NeoForge,
        _ => return None,
    };
    Some(Loader { kind, version: c.version.clone()? })
}

fn read_instance(dir: &Path) -> Option<Found> {
    let pack: Pack = serde_json::from_slice(&std::fs::read(dir.join("mmc-pack.json")).ok()?).ok()?;
    let mc_version = pack.components.iter().find(|c| c.uid == "net.minecraft")?.version.clone()?;
    let loader = pack.components.iter().find_map(loader_of);
    let cfg = std::fs::read_to_string(dir.join("instance.cfg")).unwrap_or_default();
    let name = cfg
        .lines()
        .find_map(|l| l.strip_prefix("name="))
        .map(str::to_string)
        .unwrap_or_else(|| dir.file_name().unwrap_or_default().to_string_lossy().into_owned());
    let game = ["minecraft", ".minecraft"].iter().map(|n| dir.join(n)).find(|p| p.is_dir());
    Some(Found { source: "prism", path: dir.to_string_lossy().into_owned(), name, mc_version, loader, game, only: None })
}

fn instance_dir(root: &Path) -> PathBuf {
    let cfg = ["prismlauncher.cfg", "multimc.cfg"].iter().find_map(|n| std::fs::read_to_string(root.join(n)).ok()).unwrap_or_default();
    let dir = cfg.lines().find_map(|l| l.strip_prefix("InstanceDir=")).map(str::trim).filter(|d| !d.is_empty()).unwrap_or("instances");
    root.join(dir)
}

fn list_prism() -> Vec<Found> {
    prism_roots()
        .iter()
        .filter_map(|r| std::fs::read_dir(instance_dir(r)).ok())
        .flatten()
        .flatten()
        .filter_map(|e| read_instance(&e.path()))
        .collect()
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct CfInstance {
    name: String,
    game_version: String,
    base_mod_loader: Option<CfLoader>,
}

#[derive(Deserialize)]
struct CfLoader {
    name: String,
}

pub fn curseforge_loader(id: &str, mc: &str) -> Option<Loader> {
    let (kind, version) = id.split_once('-')?;
    let kind = match kind {
        "fabric" => LoaderKind::Fabric,
        "quilt" => LoaderKind::Quilt,
        "forge" => LoaderKind::Forge,
        "neoforge" => LoaderKind::NeoForge,
        _ => return None,
    };
    let version = if kind == LoaderKind::Forge { format!("{mc}-{version}") } else { version.to_string() };
    Some(Loader { kind, version })
}

fn curseforge_roots() -> Vec<PathBuf> {
    let home = home();
    vec![home.join("curseforge/minecraft/Instances"), home.join("Documents/curseforge/minecraft/Instances")]
}

fn read_curseforge(dir: &Path) -> Option<Found> {
    let cf: CfInstance = serde_json::from_slice(&std::fs::read(dir.join("minecraftinstance.json")).ok()?).ok()?;
    let loader = cf.base_mod_loader.and_then(|l| curseforge_loader(&l.name, &cf.game_version));
    Some(Found {
        source: "curseforge",
        path: dir.to_string_lossy().into_owned(),
        name: cf.name,
        mc_version: cf.game_version,
        loader,
        game: Some(dir.to_path_buf()),
        only: None,
    })
}

fn list_curseforge() -> Vec<Found> {
    curseforge_roots().iter().filter_map(|r| std::fs::read_dir(r).ok()).flatten().flatten().filter_map(|e| read_curseforge(&e.path())).collect()
}

#[derive(Deserialize)]
struct AtlInstance {
    id: String,
    launcher: AtlLauncher,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct AtlLauncher {
    name: String,
    #[serde(default)]
    loader_version: Option<AtlLoader>,
}

#[derive(Deserialize)]
struct AtlLoader {
    version: String,
    #[serde(rename = "type")]
    kind: String,
}

fn atlauncher_roots() -> Vec<PathBuf> {
    let home = home();
    if cfg!(target_os = "windows") {
        return std::env::var_os("APPDATA").map(|a| vec![PathBuf::from(a).join("ATLauncher")]).unwrap_or_default();
    }
    if cfg!(target_os = "macos") {
        return vec![home.join("Library/Application Support/ATLauncher"), home.join("Applications/ATLauncher")];
    }
    let data = std::env::var_os("XDG_DATA_HOME").map(PathBuf::from).unwrap_or_else(|| home.join(".local/share"));
    vec![
        data.join("atlauncher"),
        data.join("ATLauncher"),
        home.join("ATLauncher"),
        home.join(".var/app/com.atlauncher.ATLauncher/data/atlauncher"),
    ]
}

fn prefixed(kind: LoaderKind, mc: &str, version: &str) -> String {
    let needs_mc = kind == LoaderKind::Forge || (kind == LoaderKind::NeoForge && mc == "1.20.1");
    if needs_mc && !version.starts_with(&format!("{mc}-")) { format!("{mc}-{version}") } else { version.to_string() }
}

fn read_atlauncher(dir: &Path) -> Option<Found> {
    let atl: AtlInstance = serde_json::from_slice(&std::fs::read(dir.join("instance.json")).ok()?).ok()?;
    let loader = atl.launcher.loader_version.and_then(|l| {
        let kind = match l.kind.to_ascii_lowercase().as_str() {
            "fabric" => LoaderKind::Fabric,
            "quilt" => LoaderKind::Quilt,
            "forge" => LoaderKind::Forge,
            "neoforge" => LoaderKind::NeoForge,
            "legacyfabric" => LoaderKind::LegacyFabric,
            _ => return None,
        };
        Some(Loader { kind, version: prefixed(kind, &atl.id, &l.version) })
    });
    Some(Found {
        source: "atlauncher",
        path: dir.to_string_lossy().into_owned(),
        name: atl.launcher.name,
        mc_version: atl.id,
        loader,
        game: Some(dir.to_path_buf()),
        only: None,
    })
}

#[derive(Deserialize)]
struct GdlConfig {
    loader: GdlLoader,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct GdlLoader {
    loader_type: String,
    mc_version: String,
    #[serde(default)]
    loader_version: Option<String>,
}

fn gdlauncher_roots() -> Vec<PathBuf> {
    let home = home();
    let base = if cfg!(target_os = "windows") {
        std::env::var_os("APPDATA").map(PathBuf::from).unwrap_or_default()
    } else if cfg!(target_os = "macos") {
        home.join("Library/Application Support")
    } else {
        std::env::var_os("XDG_CONFIG_HOME").map(PathBuf::from).unwrap_or_else(|| home.join(".config"))
    };
    vec![base.join("gdlauncher_next")]
}

fn read_gdlauncher(dir: &Path) -> Option<Found> {
    let cfg: GdlConfig = serde_json::from_slice(&std::fs::read(dir.join("config.json")).ok()?).ok()?;
    let mc = cfg.loader.mc_version;
    let loader = match (cfg.loader.loader_type.as_str(), cfg.loader.loader_version) {
        ("fabric", Some(v)) => Some(Loader { kind: LoaderKind::Fabric, version: v }),
        ("forge", Some(v)) => Some(Loader { kind: LoaderKind::Forge, version: prefixed(LoaderKind::Forge, &mc, &v) }),
        ("vanilla", _) => None,
        _ => return None,
    };
    Some(Found {
        source: "gdlauncher",
        path: dir.to_string_lossy().into_owned(),
        name: dir.file_name()?.to_string_lossy().into_owned(),
        mc_version: mc,
        loader,
        game: Some(dir.to_path_buf()),
        only: None,
    })
}

fn list_in(roots: Vec<PathBuf>, read: fn(&Path) -> Option<Found>) -> Vec<Found> {
    roots.iter().filter_map(|r| std::fs::read_dir(r.join("instances")).ok()).flatten().flatten().filter_map(|e| read(&e.path())).collect()
}

fn vanilla_root() -> PathBuf {
    if cfg!(target_os = "windows") {
        std::env::var_os("APPDATA").map(PathBuf::from).unwrap_or_default().join(".minecraft")
    } else if cfg!(target_os = "macos") {
        home().join("Library/Application Support/minecraft")
    } else {
        home().join(".minecraft")
    }
}

#[derive(Deserialize)]
struct LauncherProfiles {
    profiles: std::collections::HashMap<String, LauncherProfile>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct LauncherProfile {
    #[serde(default)]
    name: String,
    #[serde(default)]
    r#type: String,
    #[serde(default)]
    last_version_id: String,
    game_dir: Option<PathBuf>,
}

fn vanilla_version(root: &Path, id: &str) -> Option<(String, Option<Loader>)> {
    let json: serde_json::Value = std::fs::read(root.join("versions").join(id).join(format!("{id}.json")))
        .ok()
        .and_then(|b| serde_json::from_slice(&b).ok())
        .unwrap_or_default();
    let mc = json["inheritsFrom"].as_str().or_else(|| json["jar"].as_str()).map(str::to_string).or_else(|| lib_mc(&json)).unwrap_or_else(|| id.to_string());
    let loader = if let Some(rest) = id.strip_prefix("fabric-loader-").filter(|r| r.contains("-ornithe-gen")) {
        rest.split_once(&format!("-{mc}-ornithe-gen")).map(|(v, _)| Loader { kind: LoaderKind::Ornithe, version: v.into() })
    } else if let Some(rest) = id.strip_prefix("fabric-loader-") {
        rest.strip_suffix(&format!("-{mc}")).map(|v| Loader { kind: LoaderKind::Fabric, version: v.into() })
    } else if let Some(rest) = id.strip_prefix("quilt-loader-") {
        rest.strip_suffix(&format!("-{mc}")).map(|v| Loader { kind: LoaderKind::Quilt, version: v.into() })
    } else if let Some(v) = id.strip_prefix("neoforge-") {
        Some(Loader { kind: LoaderKind::NeoForge, version: v.into() })
    } else {
        id.strip_prefix(&format!("{mc}-forge-")).map(|v| Loader { kind: LoaderKind::Forge, version: format!("{mc}-{v}") })
    }
    .or_else(|| lib_loader(&json, &mc));
    if json.is_null() && (loader.is_some() || id.contains(['-', ' '])) {
        return None;
    }
    Some((mc, loader))
}

fn libs(json: &serde_json::Value) -> impl Iterator<Item = (&str, &str)> {
    json["libraries"].as_array().into_iter().flatten().filter_map(|l| {
        let mut parts = l["name"].as_str()?.split(':');
        let (group, name, version) = (parts.next()?, parts.next()?, parts.next()?);
        Some((&l["name"].as_str()?[..group.len() + 1 + name.len()], version))
    })
}

fn lib_mc(json: &serde_json::Value) -> Option<String> {
    libs(json).find_map(|(lib, v)| match lib {
        "net.fabricmc:intermediary" | "org.quiltmc:hashed" => Some(v.to_string()),
        "net.minecraftforge:forge" | "net.minecraftforge:fmlloader" => v.split_once('-').map(|(mc, _)| mc.to_string()),
        _ => None,
    })
}

fn lib_loader(json: &serde_json::Value, mc: &str) -> Option<Loader> {
    libs(json).find_map(|(lib, v)| {
        let kind = match lib {
            "org.quiltmc:quilt-loader" => LoaderKind::Quilt,
            "net.fabricmc:fabric-loader" => LoaderKind::Fabric,
            "net.neoforged:neoforge" => LoaderKind::NeoForge,
            "net.minecraftforge:forge" | "net.minecraftforge:fmlloader" => LoaderKind::Forge,
            _ => return None,
        };
        Some(Loader { kind, version: prefixed(kind, mc, v) })
    })
}

fn latest(root: &Path, kind: &str) -> Option<String> {
    let m: serde_json::Value = serde_json::from_slice(&std::fs::read(root.join("versions/version_manifest_v2.json")).ok()?).ok()?;
    m["latest"][kind].as_str().map(str::to_string)
}

fn list_vanilla() -> Vec<Found> {
    let root = vanilla_root();
    let mut out = list_profiles(&root);
    let used: Vec<_> = out.iter().map(|f| (f.mc_version.clone(), f.loader.clone())).collect();
    let (source, all) = match third_party(&root) {
        Some(s) => (s, true),
        None => ("minecraft", false),
    };
    out.extend(list_versions(&root, source).into_iter().filter(|f| (all || f.loader.is_some()) && !used.contains(&(f.mc_version.clone(), f.loader.clone()))));
    if let Some(dir) = tlauncher_gamedir().filter(|d| *d != root) {
        out.extend(list_versions(&dir, "tlauncher"));
    }
    out
}

fn tlauncher_home() -> Option<PathBuf> {
    let base = std::env::var_os("APPDATA").map(PathBuf::from).unwrap_or_else(home);
    [base.join(".tlauncher"), home().join(".tlauncher")].into_iter().find(|p| p.is_dir())
}

fn tlauncher_gamedir() -> Option<PathBuf> {
    let props = std::fs::read_to_string(tlauncher_home()?.join("tlauncher-2.0.properties")).ok()?;
    let dir = props.lines().find_map(|l| l.strip_prefix("minecraft.gamedir="))?.trim().replace("\\:", ":").replace("\\\\", "\\");
    Some(PathBuf::from(dir)).filter(|d| d.is_dir())
}

fn third_party(root: &Path) -> Option<&'static str> {
    if tlauncher_home().is_some() || root.join("TlauncherProfiles.json").is_file() {
        return Some("tlauncher");
    }
    let base = std::env::var_os("APPDATA").map(PathBuf::from).unwrap_or_else(home);
    [base.join("sklauncher"), home().join("sklauncher"), root.join("sklauncher")].iter().any(|p| p.is_dir()).then_some("sklauncher")
}

fn list_versions(root: &Path, source: &'static str) -> Vec<Found> {
    let Ok(rd) = std::fs::read_dir(root.join("versions")) else { return vec![] };
    rd.flatten()
        .filter_map(|e| {
            let id = e.file_name().to_string_lossy().into_owned();
            if !e.path().join(format!("{id}.json")).is_file() {
                return None;
            }
            let (mc_version, loader) = vanilla_version(root, &id)?;
            Some(Found {
                source,
                path: e.path().to_string_lossy().into_owned(),
                name: id,
                mc_version,
                loader,
                game: Some(root.to_path_buf()),
                only: Some(VANILLA_ONLY),
            })
        })
        .collect()
}

fn list_profiles(root: &Path) -> Vec<Found> {
    let file = root.join("launcher_profiles.json");
    let Some(lp) = std::fs::read(&file).ok().and_then(|b| serde_json::from_slice::<LauncherProfiles>(&b).ok()) else { return vec![] };
    let mut out = vec![];
    for (key, p) in lp.profiles {
        let id = match p.r#type.as_str() {
            "latest-release" => latest(root, "release"),
            "latest-snapshot" => latest(root, "snapshot"),
            _ => Some(p.last_version_id.clone()).filter(|v| !v.is_empty()),
        };
        let Some((mc_version, loader)) = id.and_then(|id| vanilla_version(root, &id)) else { continue };
        let custom = p.game_dir.filter(|d| d.is_absolute() && d.is_dir() && d != root);
        let name = if p.name.is_empty() { format!("Minecraft {mc_version}") } else { p.name };
        out.push(Found {
            source: "vanilla",
            path: format!("{}#{key}", file.display()),
            name,
            mc_version,
            loader,
            only: if custom.is_some() { None } else { Some(VANILLA_ONLY) },
            game: Some(custom.unwrap_or_else(|| root.to_path_buf())),
        });
    }
    out
}

fn modrinth_roots() -> Vec<PathBuf> {
    let home = std::env::var_os("HOME").map(PathBuf::from).unwrap_or_default();
    let data = if cfg!(target_os = "windows") {
        std::env::var_os("APPDATA").map(PathBuf::from).unwrap_or_default()
    } else if cfg!(target_os = "macos") {
        home.join("Library/Application Support")
    } else {
        std::env::var_os("XDG_DATA_HOME").map(PathBuf::from).unwrap_or_else(|| home.join(".local/share"))
    };
    vec![data.join("ModrinthApp"), data.join("com.modrinth.theseus")]
}

fn modrinth_loader(kind: &str, version: Option<String>) -> Option<Loader> {
    let kind = match kind {
        "fabric" => LoaderKind::Fabric,
        "quilt" => LoaderKind::Quilt,
        "forge" => LoaderKind::Forge,
        "neoforge" => LoaderKind::NeoForge,
        "ornithe" => LoaderKind::Ornithe,
        _ => return None,
    };
    Some(Loader { kind, version: version? })
}

type ModrinthRow = (String, String, String, String, Option<String>);

fn modrinth_rows(db: &Path) -> rusqlite::Result<Vec<ModrinthRow>> {
    let conn = rusqlite::Connection::open_with_flags(db, rusqlite::OpenFlags::SQLITE_OPEN_READ_ONLY)?;
    let has = |t: &str| {
        conn.query_row("SELECT count(*) FROM sqlite_master WHERE type='table' AND name=?1", [t], |r| r.get::<_, i64>(0))
            .map(|n| n > 0)
    };
    let sql = if has("instances")? && has("instance_content_sets")? {
        "SELECT i.name, i.path, s.game_version, s.loader, s.loader_version FROM instances i \
         JOIN instance_content_sets s ON s.id = i.applied_content_set_id"
    } else if has("profiles")? {
        "SELECT name, path, game_version, mod_loader, mod_loader_version FROM profiles"
    } else {
        return Ok(vec![]);
    };
    let mut stmt = conn.prepare(sql)?;
    let rows = stmt.query_map([], |r| Ok((r.get(0)?, r.get(1)?, r.get(2)?, r.get(3)?, r.get(4)?)))?;
    rows.collect()
}

fn list_modrinth() -> Vec<Found> {
    let mut out = vec![];
    for root in modrinth_roots() {
        let Ok(rows) = modrinth_rows(&root.join("app.db")) else { continue };
        for (name, folder, mc_version, loader, loader_version) in rows {
            let dir = root.join("profiles").join(&folder);
            if folder.contains("..") || folder.contains('/') || folder.contains('\\') || !dir.is_dir() {
                continue;
            }
            out.push(Found {
                source: "modrinth",
                path: dir.to_string_lossy().into_owned(),
                name,
                mc_version,
                loader: modrinth_loader(&loader, loader_version),
                game: Some(dir),
                only: None,
            });
        }
    }
    out
}

static SEEN: std::sync::Mutex<Vec<Found>> = std::sync::Mutex::new(Vec::new());

fn remember(found: &[Found]) {
    let mut seen = SEEN.lock().unwrap();
    seen.retain(|s| !found.iter().any(|f| f.path == s.path));
    seen.extend(found.iter().cloned());
}

pub fn scan(dir: &Path) -> Vec<Found> {
    let readers: [fn(&Path) -> Option<Found>; 4] = [read_instance, read_curseforge, read_atlauncher, read_gdlauncher];
    let mut out: Vec<Found> = readers.iter().find_map(|r| r(dir)).into_iter().collect();
    if out.is_empty() {
        for sub in [dir.to_path_buf(), dir.join("instances"), instance_dir(dir)] {
            let Ok(rd) = std::fs::read_dir(&sub) else { continue };
            out.extend(rd.flatten().filter_map(|e| readers.iter().find_map(|r| r(&e.path()))));
        }
        out.extend(list_profiles(dir));
        let used: Vec<_> = out.iter().map(|f| (f.mc_version.clone(), f.loader.clone())).collect();
        out.extend(list_versions(dir, "folder").into_iter().filter(|f| !used.contains(&(f.mc_version.clone(), f.loader.clone()))));
    }
    out.sort_by(|a, b| a.path.cmp(&b.path));
    out.dedup_by(|a, b| a.path == b.path);
    out.sort_by_key(|a| a.name.to_lowercase());
    remember(&out);
    out
}

pub fn list_all() -> Vec<Found> {
    let mut out = list_prism();
    out.extend(list_modrinth());
    out.extend(list_curseforge());
    out.extend(list_vanilla());
    out.extend(list_in(atlauncher_roots(), read_atlauncher));
    out.extend(list_in(gdlauncher_roots(), read_gdlauncher));
    out.dedup_by(|a, b| a.path == b.path);
    out.sort_by_key(|a| a.name.to_lowercase());
    remember(&out);
    out
}

fn files(dir: &Path, only: Option<&[&str]>) -> Vec<PathBuf> {
    let mut out = vec![];
    let mut stack = vec![PathBuf::new()];
    while let Some(rel) = stack.pop() {
        let Ok(rd) = std::fs::read_dir(dir.join(&rel)) else { continue };
        for e in rd.flatten() {
            let Ok(ft) = e.file_type() else { continue };
            let r = rel.join(e.file_name());
            if rel.as_os_str().is_empty() && only.is_some_and(|o| !o.iter().any(|s| e.file_name() == *s)) {
                continue;
            }
            if ft.is_dir() && !(rel.as_os_str().is_empty() && SKIP.iter().any(|s| e.file_name() == *s)) {
                stack.push(r);
            } else if ft.is_file() {
                out.push(r);
            }
        }
    }
    out
}

pub async fn import(path: &str, job: &Arc<Job>) -> Result<Instance, String> {
    let seen = SEEN.lock().unwrap().iter().find(|f| f.path == path).cloned();
    let found = seen.or_else(|| list_all().into_iter().find(|f| f.path == path)).ok_or("not an instance we can import")?;
    let inst = instances::create(&found.name, &found.mc_version, found.loader.clone())?;
    let Some(src) = found.game.clone() else { return Ok(inst) };
    let dest = instances::game_dir(&inst.id)?;

    job.progress("scan", 0, None);
    let list = tokio::task::spawn_blocking({
        let (src, only) = (src.clone(), found.only);
        move || files(&src, only)
    })
    .await
    .map_err(|e| e.to_string())?;
    let total = list.len() as u64;
    for (i, rel) in list.iter().enumerate() {
        if job.is_cancelled() {
            return Err("cancelled".into());
        }
        let to = dest.join(rel);
        if let Some(parent) = to.parent() {
            tokio::fs::create_dir_all(parent).await.map_err(|e| e.to_string())?;
        }
        tokio::fs::copy(src.join(rel), &to).await.map_err(|e| format!("{}: {e}", rel.display()))?;
        job.progress("copy", i as u64 + 1, Some(total));
    }
    Ok(inst)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn reads_modrinth_databases() {
        let dir = std::env::temp_dir().join(format!("kromvia-mr-{}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        let new = dir.join("new.db");
        let c = rusqlite::Connection::open(&new).unwrap();
        c.execute_batch(
            "CREATE TABLE instances (id TEXT, path TEXT, applied_content_set_id TEXT, name TEXT);
             CREATE TABLE instance_content_sets (id TEXT, instance_id TEXT, game_version TEXT, loader TEXT, loader_version TEXT);
             INSERT INTO instances VALUES ('i1', 'Fabulously Optimized', 's1', 'Fabulously Optimized');
             INSERT INTO instance_content_sets VALUES ('s1', 'i1', '1.21.11', 'fabric', '0.19.5');",
        )
        .unwrap();
        let old = dir.join("old.db");
        let c = rusqlite::Connection::open(&old).unwrap();
        c.execute_batch(
            "CREATE TABLE profiles (name TEXT, path TEXT, game_version TEXT, mod_loader TEXT, mod_loader_version TEXT);
             INSERT INTO profiles VALUES ('Vanilla', 'Vanilla', '1.20.1', 'vanilla', NULL);",
        )
        .unwrap();
        let rows = modrinth_rows(&new).unwrap();
        assert_eq!(rows[0].2, "1.21.11");
        assert_eq!(modrinth_loader(&rows[0].3, rows[0].4.clone()), Some(Loader { kind: LoaderKind::Fabric, version: "0.19.5".into() }));
        let rows = modrinth_rows(&old).unwrap();
        assert_eq!((rows[0].0.as_str(), modrinth_loader(&rows[0].3, rows[0].4.clone())), ("Vanilla", None));
        std::fs::remove_dir_all(dir).unwrap();
    }

    #[test]
    fn reads_prism_metadata() {
        let dir = std::env::temp_dir().join(format!("kromvia-prism-{}", std::process::id()));
        std::fs::create_dir_all(dir.join("minecraft/mods")).unwrap();
        std::fs::write(
            dir.join("mmc-pack.json"),
            r#"{"components":[{"uid":"net.minecraft","version":"1.21.11"},{"uid":"net.fabricmc.fabric-loader","version":"0.19.5"}]}"#,
        )
        .unwrap();
        std::fs::write(dir.join("instance.cfg"), "[General]\nname=My PvP\n").unwrap();
        std::fs::write(dir.join("minecraft/mods/a.jar"), b"x").unwrap();
        std::fs::create_dir_all(dir.join("minecraft/logs")).unwrap();
        std::fs::write(dir.join("minecraft/logs/latest.log"), b"x").unwrap();

        let f = read_instance(&dir).unwrap();
        assert_eq!(f.name, "My PvP");
        assert_eq!(f.mc_version, "1.21.11");
        assert_eq!(f.loader, Some(Loader { kind: LoaderKind::Fabric, version: "0.19.5".into() }));
        let copied = files(f.game.as_ref().unwrap(), None);
        assert_eq!(copied, vec![PathBuf::from("mods/a.jar")]);
        std::fs::remove_dir_all(dir).unwrap();
    }

    #[test]
    fn reads_curseforge_and_vanilla() {
        let dir = std::env::temp_dir().join(format!("kromvia-cf-{}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        std::fs::write(
            dir.join("minecraftinstance.json"),
            r#"{"name":"All the Mods","gameVersion":"1.20.1","baseModLoader":{"name":"forge-47.2.0","minecraftVersion":"1.20.1"}}"#,
        )
        .unwrap();
        let f = read_curseforge(&dir).unwrap();
        assert_eq!((f.name.as_str(), f.mc_version.as_str()), ("All the Mods", "1.20.1"));
        assert_eq!(f.loader, Some(Loader { kind: LoaderKind::Forge, version: "1.20.1-47.2.0".into() }));
        assert_eq!(curseforge_loader("neoforge-21.1.80", "1.21.1"), Some(Loader { kind: LoaderKind::NeoForge, version: "21.1.80".into() }));

        let id = "fabric-loader-0.16.10-1.21.4";
        std::fs::create_dir_all(dir.join("versions").join(id)).unwrap();
        std::fs::write(dir.join("versions").join(id).join(format!("{id}.json")), r#"{"inheritsFrom":"1.21.4"}"#).unwrap();
        assert_eq!(vanilla_version(&dir, id), Some(("1.21.4".into(), Some(Loader { kind: LoaderKind::Fabric, version: "0.16.10".into() }))));
        assert_eq!(vanilla_version(&dir, "1.21.4"), Some(("1.21.4".into(), None)));
        assert_eq!(vanilla_version(&dir, "1.20.1-forge-47.3.0"), None);

        std::fs::create_dir_all(dir.join("mods")).unwrap();
        std::fs::write(dir.join("mods/a.jar"), b"x").unwrap();
        std::fs::create_dir_all(dir.join("libraries/x")).unwrap();
        std::fs::write(dir.join("libraries/x/y.jar"), b"x").unwrap();
        assert_eq!(files(&dir, Some(VANILLA_ONLY)), vec![PathBuf::from("mods/a.jar")]);
        std::fs::remove_dir_all(dir).unwrap();
    }

    #[test]
    fn reads_full_version_jsons() {
        let dir = std::env::temp_dir().join(format!("kromvia-tl-{}", std::process::id()));
        let put = |id: &str, json: &str| {
            std::fs::create_dir_all(dir.join("versions").join(id)).unwrap();
            std::fs::write(dir.join("versions").join(id).join(format!("{id}.json")), json).unwrap();
        };
        put("Fabric 1.20.1", r#"{"libraries":[{"name":"net.fabricmc:intermediary:1.20.1"},{"name":"net.fabricmc:fabric-loader:0.15.11"}]}"#);
        put("ForgeOptiFine 1.20.1", r#"{"libraries":[{"name":"net.minecraftforge:fmlloader:1.20.1-47.3.0"}]}"#);
        put("NeoForge 1.21.1", r#"{"inheritsFrom":"1.21.1","libraries":[{"name":"net.neoforged:neoforge:21.1.80:universal"}]}"#);
        assert_eq!(vanilla_version(&dir, "Fabric 1.20.1"), Some(("1.20.1".into(), Some(Loader { kind: LoaderKind::Fabric, version: "0.15.11".into() }))));
        assert_eq!(vanilla_version(&dir, "ForgeOptiFine 1.20.1"), Some(("1.20.1".into(), Some(Loader { kind: LoaderKind::Forge, version: "1.20.1-47.3.0".into() }))));
        assert_eq!(vanilla_version(&dir, "NeoForge 1.21.1").unwrap().1.unwrap().version, "21.1.80");
        let found = scan(&dir);
        assert_eq!(found.len(), 3);
        assert!(found.iter().all(|f| f.source == "folder"));
        std::fs::remove_dir_all(dir).unwrap();
    }

    #[test]
    fn reads_atlauncher_and_gdlauncher() {
        let dir = std::env::temp_dir().join(format!("kromvia-atl-{}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        std::fs::write(
            dir.join("instance.json"),
            r#"{"id":"1.20.1","launcher":{"name":"Create Above","loaderVersion":{"version":"47.2.0","rawVersion":"1.20.1-47.2.0","recommended":true,"type":"Forge"}}}"#,
        )
        .unwrap();
        let f = read_atlauncher(&dir).unwrap();
        assert_eq!((f.name.as_str(), f.mc_version.as_str()), ("Create Above", "1.20.1"));
        assert_eq!(f.loader, Some(Loader { kind: LoaderKind::Forge, version: "1.20.1-47.2.0".into() }));
        std::fs::write(dir.join("instance.json"), r#"{"id":"1.21.4","launcher":{"name":"Plain"}}"#).unwrap();
        assert_eq!(read_atlauncher(&dir).unwrap().loader, None);

        std::fs::write(dir.join("config.json"), r#"{"loader":{"loaderType":"fabric","mcVersion":"1.16.5","loaderVersion":"0.11.3"}}"#).unwrap();
        let g = read_gdlauncher(&dir).unwrap();
        assert_eq!(g.mc_version, "1.16.5");
        assert_eq!(g.loader, Some(Loader { kind: LoaderKind::Fabric, version: "0.11.3".into() }));
        std::fs::write(dir.join("config.json"), r#"{"loader":{"loaderType":"forge","mcVersion":"1.12.2","loaderVersion":"1.12.2-14.23.5.2860"}}"#).unwrap();
        assert_eq!(read_gdlauncher(&dir).unwrap().loader.unwrap().version, "1.12.2-14.23.5.2860");
        std::fs::remove_dir_all(dir).unwrap();
    }
}
