
use std::collections::{HashMap, HashSet};
use std::path::{Path, PathBuf};
use std::sync::{Arc, Mutex};

use serde::Serialize;

use crate::engine::content::FOLDERS;
use crate::engine::core::http;
use crate::engine::core::jobs::Job;
use crate::engine::core::paths;
use crate::engine::instances;
use crate::engine::worlds::dir_size;

#[derive(Serialize, Clone, Copy, Default)]
#[serde(rename_all = "camelCase")]
pub struct Dedup {
    pub files: u64,
    pub saved: u64,
    pub removed: u64,
}

static LAST: Mutex<Option<Dedup>> = Mutex::new(None);

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct Usage {
    pub instances: u64,
    pub versions: u64,
    pub libraries: u64,
    pub assets: u64,
    pub runtimes: u64,
    pub backups: u64,
    pub temp: u64,
    pub last_dedup: Option<Dedup>,
}

pub fn usage() -> Usage {
    let root = paths::root();
    Usage {
        instances: dir_size(&paths::instances()),
        versions: dir_size(&paths::versions()),
        libraries: dir_size(&paths::libraries()),
        assets: dir_size(&paths::assets()),
        runtimes: dir_size(&paths::runtimes()),
        backups: dir_size(&root.join("backups")),
        temp: dir_size(&paths::temp()),
        last_dedup: *LAST.lock().unwrap(),
    }
}

pub fn clear_temp() -> Result<u64, String> {
    let dir = paths::temp();
    let size = dir_size(&dir);
    if dir.exists() {
        std::fs::remove_dir_all(&dir).map_err(|e| e.to_string())?;
    }
    std::fs::create_dir_all(&dir).map_err(|e| e.to_string())?;
    Ok(size)
}

fn content_files() -> Vec<PathBuf> {
    let mut out = vec![];
    for inst in instances::list() {
        let Ok(game) = instances::game_dir(&inst.id) else { continue };
        for folder in FOLDERS {
            for e in std::fs::read_dir(game.join(folder)).into_iter().flatten().flatten() {
                if e.file_type().is_ok_and(|t| t.is_file()) && !e.file_name().to_string_lossy().ends_with(".part") {
                    out.push(e.path());
                }
            }
        }
    }
    out
}

#[cfg(unix)]
fn same_file(a: &Path, b: &Path) -> bool {
    use std::os::unix::fs::MetadataExt;
    matches!((std::fs::metadata(a), std::fs::metadata(b)), (Ok(x), Ok(y)) if (x.dev(), x.ino()) == (y.dev(), y.ino()))
}

#[cfg(not(unix))]
fn same_file(_: &Path, _: &Path) -> bool {
    false
}

fn relink(store: &Path, path: &Path) -> std::io::Result<()> {
    let mut tmp = path.as_os_str().to_os_string();
    tmp.push(".link");
    let tmp = PathBuf::from(tmp);
    let _ = std::fs::remove_file(&tmp);
    std::fs::hard_link(store, &tmp)?;
    std::fs::rename(&tmp, path)
}

fn dedup_in(store: &Path, files: &[PathBuf], job: &Job) -> Result<Dedup, String> {
    let mut used = HashSet::new();
    let mut unique: HashMap<String, u64> = HashMap::new();
    let (mut logical, mut stats) = (0u64, Dedup::default());
    let total = files.len() as u64;
    for (i, path) in files.iter().enumerate() {
        if job.is_cancelled() {
            return Err("cancelled".into());
        }
        job.progress("dedup", i as u64, Some(total));
        let Ok(size) = std::fs::metadata(path).map(|m| m.len()) else { continue };
        let sha1 = http::sha1_file(path)?;
        let obj = store.join(&sha1[..2]).join(&sha1);
        if obj.is_file() {
            if !same_file(path, &obj) {
                relink(&obj, path).map_err(|e| format!("{}: {e}", path.display()))?;
            }
        } else {
            std::fs::create_dir_all(obj.parent().unwrap()).map_err(|e| e.to_string())?;
            std::fs::hard_link(path, &obj).map_err(|e| format!("this disk does not support hard links: {e}"))?;
        }
        used.insert(sha1.clone());
        unique.insert(sha1, size);
        logical += size;
        stats.files += 1;
    }
    for dir in std::fs::read_dir(store).into_iter().flatten().flatten() {
        for e in std::fs::read_dir(dir.path()).into_iter().flatten().flatten() {
            if !used.contains(e.file_name().to_string_lossy().as_ref()) && std::fs::remove_file(e.path()).is_ok() {
                stats.removed += 1;
            }
        }
    }
    stats.saved = logical - unique.values().sum::<u64>();
    Ok(stats)
}

pub async fn dedup(job: &Arc<Job>) -> Result<Dedup, String> {
    job.progress("scan", 0, None);
    let job = job.clone();
    let stats = tokio::task::spawn_blocking(move || dedup_in(&paths::objects(), &content_files(), &job)).await.map_err(|e| e.to_string())??;
    *LAST.lock().unwrap() = Some(stats);
    Ok(stats)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn links_duplicates_and_drops_unused() {
        let dir = std::env::temp_dir().join(format!("kromvia-dedup-{}", std::process::id()));
        let (a, b, store) = (dir.join("a"), dir.join("b"), dir.join("objects"));
        for d in [&a, &b] {
            std::fs::create_dir_all(d).unwrap();
            std::fs::write(d.join("sodium.jar"), vec![7u8; 1000]).unwrap();
        }
        std::fs::write(a.join("other.jar"), b"other").unwrap();
        std::fs::create_dir_all(store.join("ff")).unwrap();
        std::fs::write(store.join("ff/ffff"), b"stale").unwrap();

        let files = vec![a.join("sodium.jar"), b.join("sodium.jar"), a.join("other.jar")];
        let job = Job::detached("dedup");
        let s = dedup_in(&store, &files, &job).unwrap();
        assert_eq!((s.files, s.saved, s.removed), (3, 1000, 1));
        #[cfg(unix)]
        assert!(same_file(&a.join("sodium.jar"), &b.join("sodium.jar")));
        assert_eq!(std::fs::read(b.join("sodium.jar")).unwrap(), vec![7u8; 1000]);

        let again = dedup_in(&store, &files, &job).unwrap();
        assert_eq!((again.saved, again.removed), (1000, 0));
        std::fs::remove_dir_all(dir).unwrap();
    }
}
