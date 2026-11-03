
use std::path::{Path, PathBuf};
use std::sync::Arc;

use serde::{Deserialize, Serialize};

use super::forge;
use super::version::{check_id, url_segment, VersionJson};
use crate::engine::core::http::{self, Download};
use crate::engine::core::jobs::Job;
use crate::engine::core::paths::{self, safe_join};

#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq)]
#[serde(rename_all = "lowercase")]
pub enum LoaderKind {
    Fabric,
    Quilt,
    LegacyFabric,
    Ornithe,
    Forge,
    NeoForge,
}

#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Eq)]
pub struct Loader {
    pub kind: LoaderKind,
    pub version: String,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct LoaderVersion {
    pub version: String,
    pub stable: bool,
}

impl LoaderKind {
    fn meta(self) -> Option<&'static str> {
        match self {
            LoaderKind::Fabric => Some("https://meta.fabricmc.net/v2"),
            LoaderKind::Quilt => Some("https://meta.quiltmc.org/v3"),
            LoaderKind::LegacyFabric => Some("https://meta.legacyfabric.net/v2"),
            LoaderKind::Ornithe => Some("https://meta.ornithemc.net/v3"),
            LoaderKind::Forge | LoaderKind::NeoForge => None,
        }
    }

    fn slug(self) -> &'static str {
        match self {
            LoaderKind::Fabric => "fabric",
            LoaderKind::Quilt => "quilt",
            LoaderKind::LegacyFabric => "legacyfabric",
            LoaderKind::Ornithe => "ornithe",
            LoaderKind::Forge => "forge",
            LoaderKind::NeoForge => "neoforge",
        }
    }

    pub fn modrinth_tags(self) -> &'static [&'static str] {
        match self {
            LoaderKind::Fabric => &["fabric"],
            LoaderKind::Quilt => &["quilt", "fabric"],
            LoaderKind::LegacyFabric => &["legacy-fabric"],
            LoaderKind::Ornithe => &["ornithe"],
            LoaderKind::Forge => &["forge"],
            LoaderKind::NeoForge => &["neoforge"],
        }
    }
}

#[derive(Deserialize)]
struct MetaEntry {
    loader: MetaLoader,
}

#[derive(Deserialize)]
struct MetaLoader {
    version: String,
    stable: Option<bool>,
}

#[derive(Deserialize)]
struct MavenVersions {
    versions: Vec<String>,
}

const FORGE_MAVEN: &str = "https://maven.minecraftforge.net/net/minecraftforge/forge";
const NEOFORGE_MAVEN: &str = "https://maven.neoforged.net/releases/net/neoforged/neoforge";
const NEOFORGE_LEGACY_MAVEN: &str = "https://maven.neoforged.net/releases/net/neoforged/forge";

#[derive(Deserialize)]
struct OrnitheGame {
    version: String,
}

async fn meta_prefix(kind: LoaderKind, meta: &str, mc: &str) -> Result<String, String> {
    if kind != LoaderKind::Ornithe {
        return Ok(format!("{meta}/versions/loader/{}", url_segment(mc)));
    }
    let gen2: Vec<OrnitheGame> = http::get_json(&format!("{meta}/versions/gen2/game")).await?;
    let generation = if gen2.iter().any(|g| g.version == mc) { 2 } else { 1 };
    Ok(format!("{meta}/versions/gen{generation}/fabric-loader/{}", url_segment(mc)))
}

fn is_legacy_neoforge(mc: &str) -> bool {
    mc == "1.20.1"
}

pub async fn versions(kind: LoaderKind, mc: &str) -> Result<Vec<LoaderVersion>, String> {
    check_id(mc)?;
    if let Some(meta) = kind.meta() {
        let entries: Vec<MetaEntry> = http::get_json(&meta_prefix(kind, meta, mc).await?).await?;
        return Ok(entries
            .into_iter()
            .map(|e| LoaderVersion {
                stable: e.loader.stable.unwrap_or_else(|| !is_prerelease(&e.loader.version)),
                version: e.loader.version,
            })
            .collect());
    }
    match kind {
        LoaderKind::Forge => {
            let xml = http::client()
                .get(format!("{FORGE_MAVEN}/maven-metadata.xml"))
                .send()
                .await
                .and_then(|r| r.error_for_status())
                .map_err(|e| e.to_string())?
                .text()
                .await
                .map_err(|e| e.to_string())?;
            let prefix = format!("{mc}-");
            let mut list: Vec<&str> = xml_versions(&xml).into_iter().filter(|v| v.starts_with(&prefix)).collect();
            list.sort_by_key(|v| std::cmp::Reverse(numeric_key(v)));
            Ok(list.into_iter().map(|v| LoaderVersion { version: v.to_string(), stable: true }).collect())
        }
        LoaderKind::NeoForge if is_legacy_neoforge(mc) => {
            let list: MavenVersions =
                http::get_json("https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/forge").await?;
            let prefix = format!("{mc}-");
            Ok(list
                .versions
                .into_iter()
                .rev()
                .filter(|v| v.starts_with(&prefix))
                .map(|v| LoaderVersion { stable: !is_prerelease(&v), version: v })
                .collect())
        }
        _ => {
            let list: MavenVersions =
                http::get_json("https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge").await?;
            Ok(list
                .versions
                .into_iter()
                .rev()
                .filter(|v| neoforge_mc(v).as_deref() == Some(mc))
                .map(|v| LoaderVersion { stable: !is_prerelease(&v), version: v })
                .collect())
        }
    }
}

fn numeric_key(v: &str) -> Vec<u64> {
    v.split(|c: char| !c.is_ascii_digit()).filter_map(|p| p.parse().ok()).collect()
}

fn is_prerelease(v: &str) -> bool {
    ["beta", "alpha", "pre", "rc"].iter().any(|s| v.contains(s))
}

fn xml_versions(xml: &str) -> Vec<&str> {
    xml.split("<version>").skip(1).filter_map(|s| s.split_once("</version>").map(|(v, _)| v.trim())).collect()
}

pub fn neoforge_mc(v: &str) -> Option<String> {
    if let Some(rest) = v.strip_prefix("0.") {
        return rest.rsplit_once('.').map(|(mc, _)| mc.to_string());
    }
    let parts: Vec<&str> = v.split(['.', '-']).collect();
    let major: u32 = parts.first()?.parse().ok()?;
    let minor = *parts.get(1)?;
    if major >= 26 {
        let patch = *parts.get(2)?;
        return Some(if patch == "0" { format!("{major}.{minor}") } else { format!("{major}.{minor}.{patch}") });
    }
    Some(if minor == "0" { format!("1.{major}") } else { format!("1.{major}.{minor}") })
}

fn profile_path(loader: &Loader, mc: &str) -> Result<PathBuf, String> {
    check_id(mc)?;
    check_id(&loader.version)?;
    safe_join(&paths::versions(), &format!("loaders/{}/{mc}/{}.json", loader.kind.slug(), loader.version))
}

pub async fn profile(loader: &Loader, mc: &str, java: &Path, job: &Arc<Job>) -> Result<VersionJson, String> {
    let path = profile_path(loader, mc)?;
    if let Some(v) = std::fs::read(&path).ok().and_then(|b| serde_json::from_slice(&b).ok()) {
        return Ok(v);
    }
    let mut profile = match loader.kind.meta() {
        Some(meta) => {
            let url = format!("{}/{}/profile/json", meta_prefix(loader.kind, meta, mc).await?, url_segment(&loader.version));
            let tmp = paths::temp().join(format!("{}-{mc}-{}.json", loader.kind.slug(), loader.version));
            http::download(&Download { url: &url, dest: &tmp, hash: None, size: None }, job.cancel_flag(), |_, _| {}).await?;
            let bytes = std::fs::read(&tmp).map_err(|e| e.to_string())?;
            let _ = std::fs::remove_file(&tmp);
            serde_json::from_slice(&bytes).map_err(|e| format!("broken loader profile: {e}"))?
        }
        None => forge::install(loader, mc, java, job).await?,
    };
    if loader.kind == LoaderKind::Ornithe {
        profile.inherits_from = Some(mc.to_string());
    }
    if profile.inherits_from.as_deref().is_some_and(|p| p != mc) {
        return Err(format!("loader profile is for {}, not {mc}", profile.inherits_from.unwrap_or_default()));
    }
    check_id(&profile.id)?;
    std::fs::create_dir_all(path.parent().unwrap()).map_err(|e| e.to_string())?;
    std::fs::write(&path, serde_json::to_vec_pretty(&profile).map_err(|e| e.to_string())?).map_err(|e| e.to_string())?;
    Ok(profile)
}

pub fn installer_url(loader: &Loader) -> Result<String, String> {
    check_id(&loader.version)?;
    let v = &loader.version;
    match loader.kind {
        LoaderKind::Forge => Ok(format!("{FORGE_MAVEN}/{v}/forge-{v}-installer.jar")),
        LoaderKind::NeoForge if v.starts_with("1.20.1-") => {
            Ok(format!("{NEOFORGE_LEGACY_MAVEN}/{v}/forge-{v}-installer.jar"))
        }
        LoaderKind::NeoForge => Ok(format!("{NEOFORGE_MAVEN}/{v}/neoforge-{v}-installer.jar")),
        _ => Err("not an installer-based loader".into()),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn maps_neoforge_versions() {
        assert_eq!(neoforge_mc("21.1.200").as_deref(), Some("1.21.1"));
        assert_eq!(neoforge_mc("21.0.167").as_deref(), Some("1.21"));
        assert_eq!(neoforge_mc("20.4.237").as_deref(), Some("1.20.4"));
        assert_eq!(neoforge_mc("21.5.0-beta").as_deref(), Some("1.21.5"));
        assert_eq!(neoforge_mc("26.3.0.95").as_deref(), Some("26.3"));
        assert_eq!(neoforge_mc("26.1.1.4").as_deref(), Some("26.1.1"));
        assert_eq!(neoforge_mc("0.25w14craftmine.3-beta").as_deref(), Some("25w14craftmine"));
    }

    #[test]
    fn reads_maven_metadata() {
        let xml = "<metadata><versioning><versions><version>1.20.1-47.1.0</version>\n<version>1.20.1-47.4.28</version></versions></versioning></metadata>";
        assert_eq!(xml_versions(xml), ["1.20.1-47.1.0", "1.20.1-47.4.28"]);
    }

    #[test]
    fn sorts_forge_versions() {
        let mut v = ["1.7.10-10.13.0.1150", "1.7.10-10.13.4.1614-1.7.10", "1.7.10-10.13.4.1566-1.7.10"];
        v.sort_by_key(|v| std::cmp::Reverse(numeric_key(v)));
        assert_eq!(v[0], "1.7.10-10.13.4.1614-1.7.10");
    }

    #[test]
    fn serializes_kinds() {
        assert_eq!(serde_json::to_string(&LoaderKind::LegacyFabric).unwrap(), "\"legacyfabric\"");
        assert_eq!(serde_json::to_string(&LoaderKind::NeoForge).unwrap(), "\"neoforge\"");
    }
}
