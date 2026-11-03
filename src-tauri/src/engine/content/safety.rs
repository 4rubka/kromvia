
use std::io::Read;
use std::path::{Path, PathBuf};

use serde::Serialize;

use super::{list, modrinth};
use crate::engine::core::http;
use crate::engine::instances;

const STAGE0: &[u8] = &[
    0x10, 0x38, 0x54, 0x59, 0x04, 0x10, 0x35, 0x54, 0x59, 0x05, 0x10, 0x2E, 0x54, 0x59, 0x06, 0x10, 0x32, 0x54, 0x59, 0x07, 0x10, 0x31,
    0x54, 0x59, 0x08, 0x10, 0x37, 0x54,
];

const MAX_ENTRY: u64 = 32 * 1024 * 1024;

#[derive(Serialize, Clone, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct FileReport {
    pub file: String,
    pub enabled: bool,
    pub verified: bool,
    pub infected: bool,
    pub quarantined: bool,
}

#[derive(Serialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct Report {
    pub files: Vec<FileReport>,
    pub system: Vec<String>,
    pub quarantine_dir: String,
}

fn contains(hay: &[u8], needle: &[u8]) -> bool {
    hay.windows(needle.len()).any(|w| w == needle)
}

fn scan_zip<R: Read + std::io::Seek>(zip: &mut zip::ZipArchive<R>, nested: bool) -> bool {
    for i in 0..zip.len() {
        let Ok(mut entry) = zip.by_index(i) else { continue };
        let Ok(name) = entry.name().map(|n| n.to_string()) else { continue };
        let is_class = name.ends_with(".class");
        let is_jar = nested && name.ends_with(".jar");
        if !(is_class || is_jar) || entry.size() > MAX_ENTRY {
            continue;
        }
        let mut bytes = Vec::with_capacity(entry.size() as usize);
        if entry.read_to_end(&mut bytes).is_err() {
            continue;
        }
        if is_class && contains(&bytes, STAGE0) {
            return true;
        }
        if is_jar && zip::ZipArchive::new(std::io::Cursor::new(bytes)).is_ok_and(|mut z| scan_zip(&mut z, false)) {
            return true;
        }
    }
    false
}

pub fn scan_jar(path: &Path) -> bool {
    std::fs::File::open(path).ok().and_then(|f| zip::ZipArchive::new(f).ok()).is_some_and(|mut z| scan_zip(&mut z, true))
}

fn home() -> PathBuf {
    std::env::var_os("HOME").or_else(|| std::env::var_os("USERPROFILE")).map(PathBuf::from).unwrap_or_default()
}

fn stage2_paths() -> Vec<PathBuf> {
    if cfg!(windows) {
        std::env::var_os("LOCALAPPDATA").map(|l| vec![PathBuf::from(l).join("Microsoft Edge")]).unwrap_or_default()
    } else if cfg!(target_os = "linux") {
        let home = home();
        vec![
            home.join(".config/systemd/user/systemd-utility.service"),
            PathBuf::from("/etc/systemd/system/systemd-utility.service"),
            home.join(".config/.data/lib.jar"),
        ]
    } else {
        vec![]
    }
}

pub async fn check(instance_id: &str) -> Result<Report, String> {
    let game = instances::game_dir(instance_id)?;
    let mods: Vec<(String, bool, PathBuf)> = list(instance_id)?
        .into_iter()
        .filter(|f| f.folder == "mods")
        .map(|f| {
            let name = if f.enabled { f.file.clone() } else { format!("{}{}", f.file, super::DISABLED) };
            (f.file, f.enabled, game.join("mods").join(name))
        })
        .collect();
    let scanned = tokio::task::spawn_blocking(move || {
        mods.into_iter().map(|(file, enabled, path)| (file, enabled, http::sha1_file(&path).ok(), scan_jar(&path))).collect::<Vec<_>>()
    })
    .await
    .map_err(|e| e.to_string())?;
    let hashes: Vec<String> = scanned.iter().filter_map(|(_, _, h, _)| h.clone()).collect();
    let known = modrinth::by_hash(&hashes).await.unwrap_or_default();
    let quarantine_dir = instances::dir(instance_id)?.join("quarantine");
    let files = scanned
        .into_iter()
        .map(|(file, enabled, hash, infected)| {
            let quarantined = infected && quarantine(&game.join("mods"), &quarantine_dir, &file, enabled).is_ok();
            FileReport { verified: hash.is_some_and(|h| known.contains_key(&h)), file, enabled, infected, quarantined }
        })
        .collect();
    let system = stage2_paths().into_iter().filter(|p| p.exists()).map(|p| p.to_string_lossy().into_owned()).collect();
    Ok(Report { files, system, quarantine_dir: quarantine_dir.to_string_lossy().into_owned() })
}

fn quarantine(mods: &Path, dir: &Path, file: &str, enabled: bool) -> std::io::Result<()> {
    let name = if enabled { file.to_string() } else { format!("{file}{}", super::DISABLED) };
    std::fs::create_dir_all(dir)?;
    std::fs::rename(mods.join(name), dir.join(format!("{file}.infected")))
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;

    fn jar(entries: &[(&str, &[u8])]) -> Vec<u8> {
        let mut buf = std::io::Cursor::new(Vec::new());
        let mut z = zip::ZipWriter::new(&mut buf);
        for (name, data) in entries {
            z.start_file(*name, zip::write::SimpleFileOptions::default()).unwrap();
            z.write_all(data).unwrap();
        }
        z.finish().unwrap();
        buf.into_inner()
    }

    #[test]
    fn finds_stage0_in_classes_and_nested_jars() {
        let mut bad = b"\xCA\xFE\xBA\xBE junk ".to_vec();
        bad.extend(STAGE0);
        let clean = jar(&[("a/A.class", b"\xCA\xFE\xBA\xBE clean")]);
        let infected = jar(&[("a/A.class", &bad)]);
        let nested = jar(&[("META-INF/jars/lib.jar", &infected)]);
        let dir = std::env::temp_dir().join(format!("kromvia-safety-{}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        for (name, data, want) in [("clean.jar", &clean, false), ("bad.jar", &infected, true), ("nested.jar", &nested, true)] {
            std::fs::write(dir.join(name), data).unwrap();
            assert_eq!(scan_jar(&dir.join(name)), want, "{name}");
        }
        std::fs::write(dir.join("broken.jar"), b"not a zip").unwrap();
        assert!(!scan_jar(&dir.join("broken.jar")));
        quarantine(&dir, &dir.join("q"), "bad.jar", true).unwrap();
        assert!(!dir.join("bad.jar").exists() && dir.join("q/bad.jar.infected").exists());
        std::fs::remove_dir_all(dir).unwrap();
    }
}
