
use std::collections::HashMap;
use std::path::{Path, PathBuf};

use crate::engine::core::jobs::Job;
use crate::engine::core::paths;

const OWNED: &[&str] = &[
    "accounts.json", "device-key", "assets", "backups", "client", "instances", "libraries", "logs", "objects", "runtimes",
    "skins", "tmp", "versions",
];

pub fn target_for(picked: &Path) -> Result<PathBuf, String> {
    if !picked.is_absolute() {
        return Err("pick a full folder path".into());
    }
    let root = paths::root();
    if picked == root {
        return Err("the data is already there".into());
    }
    if picked == paths::default_root() {
        return Ok(picked.to_path_buf());
    }
    let target = if is_empty(picked) { picked.to_path_buf() } else { picked.join("Kromvia") };
    if target.starts_with(root) || root.starts_with(&target) {
        return Err("pick a folder outside the current one".into());
    }
    if !is_empty(&target) {
        return Err(format!("{} is not empty", target.display()));
    }
    Ok(target)
}

fn is_empty(dir: &Path) -> bool {
    std::fs::read_dir(dir).map_or(true, |mut d| d.next().is_none())
}

pub fn relocate(from: &Path, to: &Path, job: &Job) -> Result<(), String> {
    let names: Vec<&str> = OWNED.iter().copied().filter(|n| from.join(n).exists()).collect();
    if let Some(n) = names.iter().find(|n| to.join(n).exists()) {
        return Err(format!("{} already has {n}", to.display()));
    }
    std::fs::create_dir_all(to).map_err(|e| e.to_string())?;

    let mut renamed = vec![];
    for n in &names {
        if std::fs::rename(from.join(n), to.join(n)).is_err() {
            break;
        }
        renamed.push(*n);
    }
    if renamed.len() == names.len() {
        return Ok(());
    }
    for n in renamed.drain(..) {
        std::fs::rename(to.join(n), from.join(n)).map_err(|e| format!("could not move {n} back: {e}"))?;
    }

    let total = names.iter().map(|n| size(&from.join(n))).sum();
    let mut copy = Copy { done: 0, total, links: HashMap::new(), job };
    let copied = names.iter().try_for_each(|n| copy.tree(&from.join(n), &to.join(n)));
    if let Err(e) = copied {
        for n in &names {
            let _ = remove(&to.join(n));
        }
        return Err(e);
    }
    for n in &names {
        let _ = remove(&from.join(n));
    }
    Ok(())
}

fn size(path: &Path) -> u64 {
    if path.is_dir() {
        crate::engine::worlds::dir_size(path)
    } else {
        std::fs::metadata(path).map_or(0, |m| m.len())
    }
}

fn remove(path: &Path) -> std::io::Result<()> {
    if path.is_dir() {
        std::fs::remove_dir_all(path)
    } else {
        std::fs::remove_file(path)
    }
}

struct Copy<'a> {
    done: u64,
    total: u64,
    links: HashMap<(u64, u64), PathBuf>,
    job: &'a Job,
}

impl Copy<'_> {
    fn tree(&mut self, from: &Path, to: &Path) -> Result<(), String> {
        if self.job.is_cancelled() {
            return Err("cancelled".into());
        }
        let meta = std::fs::symlink_metadata(from).map_err(|e| format!("{}: {e}", from.display()))?;
        if meta.is_dir() {
            std::fs::create_dir_all(to).map_err(|e| format!("{}: {e}", to.display()))?;
            for e in std::fs::read_dir(from).map_err(|e| format!("{}: {e}", from.display()))? {
                let e = e.map_err(|e| e.to_string())?;
                self.tree(&e.path(), &to.join(e.file_name()))?;
            }
            return Ok(());
        }
        if !meta.is_file() {
            return Ok(());
        }
        if let Some(id) = link_id(&meta) {
            if let Some(first) = self.links.get(&id) {
                if std::fs::hard_link(first, to).is_ok() {
                    return Ok(());
                }
            }
            self.links.insert(id, to.to_path_buf());
        }
        std::fs::copy(from, to).map_err(|e| format!("{}: {e}", from.display()))?;
        self.done += meta.len();
        self.job.progress("move", self.done, Some(self.total));
        Ok(())
    }
}

#[cfg(unix)]
fn link_id(meta: &std::fs::Metadata) -> Option<(u64, u64)> {
    use std::os::unix::fs::MetadataExt;
    (meta.nlink() > 1).then(|| (meta.dev(), meta.ino()))
}

#[cfg(not(unix))]
fn link_id(_: &std::fs::Metadata) -> Option<(u64, u64)> {
    None
}

#[cfg(test)]
mod tests {
    use super::*;

    fn tree(dir: &Path) {
        std::fs::create_dir_all(dir.join("instances/a")).unwrap();
        std::fs::write(dir.join("instances/a/mod.jar"), b"jar").unwrap();
        std::fs::write(dir.join("accounts.json"), b"[]").unwrap();
        std::fs::create_dir_all(dir.join("localstorage")).unwrap();
    }

    #[test]
    fn moves_owned_files_only() {
        let dir = std::env::temp_dir().join(format!("kromvia-move-{}", std::process::id()));
        let (from, to) = (dir.join("from"), dir.join("to"));
        tree(&from);
        relocate(&from, &to, &Job::detached("move")).unwrap();
        assert_eq!(std::fs::read(to.join("instances/a/mod.jar")).unwrap(), b"jar");
        assert!(to.join("accounts.json").is_file() && !from.join("accounts.json").exists());
        assert!(from.join("localstorage").is_dir() && !to.join("localstorage").exists());
        std::fs::remove_dir_all(dir).unwrap();
    }

    #[test]
    fn copy_keeps_hard_links() {
        let dir = std::env::temp_dir().join(format!("kromvia-copy-{}", std::process::id()));
        tree(&dir.join("from"));
        std::fs::hard_link(dir.join("from/instances/a/mod.jar"), dir.join("from/instances/a/same.jar")).unwrap();
        let job = Job::detached("move");
        let mut copy = Copy { done: 0, total: 0, links: HashMap::new(), job: &job };
        copy.tree(&dir.join("from/instances"), &dir.join("to/instances")).unwrap();
        #[cfg(unix)]
        {
            use std::os::unix::fs::MetadataExt;
            let (a, b) = (std::fs::metadata(dir.join("to/instances/a/mod.jar")).unwrap(), std::fs::metadata(dir.join("to/instances/a/same.jar")).unwrap());
            assert_eq!(a.ino(), b.ino());
        }
        std::fs::remove_dir_all(dir).unwrap();
    }
}
