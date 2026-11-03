
use std::io::Write;
use std::path::{Path, PathBuf};

use serde::Serialize;

use crate::engine::content::mrpack::walk;
use crate::engine::core::jobs::Job;
use crate::engine::core::paths::{self, safe_join};
use crate::engine::instances;

const TOP: &str = "instance";

#[derive(Serialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct Backup {
    pub name: String,
    pub created: u64,
    pub size: u64,
}

fn dir(instance_id: &str) -> Result<PathBuf, String> {
    instances::dir(instance_id)?;
    safe_join(&paths::root().join("backups"), instance_id)
}

fn file(instance_id: &str, name: &str) -> Result<PathBuf, String> {
    match name.strip_suffix(".zip") {
        Some(secs) if !secs.is_empty() && secs.bytes().all(|b| b.is_ascii_digit()) => Ok(dir(instance_id)?.join(name)),
        _ => Err(format!("bad backup name {name}")),
    }
}

pub fn list(instance_id: &str) -> Result<Vec<Backup>, String> {
    let mut out: Vec<Backup> = std::fs::read_dir(dir(instance_id)?)
        .into_iter()
        .flatten()
        .flatten()
        .filter_map(|e| {
            let name = e.file_name().to_str()?.to_string();
            let created = name.strip_suffix(".zip")?.parse().ok()?;
            Some(Backup { size: e.metadata().ok()?.len(), name, created })
        })
        .collect();
    out.sort_by_key(|b| std::cmp::Reverse(b.created));
    Ok(out)
}

fn skipped(rel: &str) -> bool {
    let game = format!("{TOP}/minecraft/");
    rel.starts_with(&format!("{game}natives/")) || rel.starts_with(&format!("{game}logs/")) || rel.ends_with("/session.lock")
}

pub fn create(instance_id: &str, job: &Job) -> Result<Backup, String> {
    let src = instances::dir(instance_id)?;
    instances::get(instance_id)?;
    let out_dir = dir(instance_id)?;
    std::fs::create_dir_all(&out_dir).map_err(|e| e.to_string())?;
    let mut created = instances::now();
    while out_dir.join(format!("{created}.zip")).exists() {
        created += 1;
    }
    let name = format!("{created}.zip");
    let dest = out_dir.join(&name);
    let part = dest.with_extension("zip.part");
    let files: Vec<_> = walk(&src, TOP).into_iter().filter(|(rel, _)| !skipped(rel)).collect();
    let result = (|| {
        let mut zip = zip::ZipWriter::new(std::fs::File::create(&part).map_err(|e| e.to_string())?);
        let opts = zip::write::SimpleFileOptions::default();
        let total = files.iter().filter_map(|(_, p)| std::fs::metadata(p).ok()).map(|m| m.len()).sum();
        let mut done = 0;
        for (rel, path) in &files {
            if job.is_cancelled() {
                return Err("cancelled".to_string());
            }
            zip.start_file(rel.as_str(), opts).map_err(|e| e.to_string())?;
            done += std::io::copy(&mut std::fs::File::open(path).map_err(|e| e.to_string())?, &mut zip).map_err(|e| e.to_string())?;
            job.progress("pack", done, Some(total));
        }
        zip.flush().map_err(|e| e.to_string())?;
        zip.finish().map_err(|e| e.to_string())?;
        Ok(())
    })();
    if let Err(e) = result {
        let _ = std::fs::remove_file(&part);
        return Err(e);
    }
    std::fs::rename(&part, &dest).map_err(|e| e.to_string())?;
    let size = std::fs::metadata(&dest).map(|m| m.len()).unwrap_or(0);
    Ok(Backup { name, created, size })
}

pub fn restore(instance_id: &str, name: &str) -> Result<(), String> {
    let archive = file(instance_id, name)?;
    let target = instances::dir(instance_id)?;
    let staging = paths::temp().join(format!("restore-{instance_id}"));
    let old = paths::temp().join(format!("restore-old-{instance_id}"));
    let _ = std::fs::remove_dir_all(&staging);
    let _ = std::fs::remove_dir_all(&old);
    std::fs::create_dir_all(&staging).map_err(|e| e.to_string())?;
    let result = (|| {
        let zip_file = std::fs::File::open(&archive).map_err(|e| e.to_string())?;
        zip::ZipArchive::new(zip_file).and_then(|mut z| z.extract(&staging)).map_err(|e| e.to_string())?;
        let restored = staging.join(TOP);
        check_restored(&restored, instance_id)?;
        swap(&target, &restored, &old)
    })();
    let _ = std::fs::remove_dir_all(&staging);
    result
}

fn check_restored(dir: &Path, instance_id: &str) -> Result<(), String> {
    let inst: instances::Instance =
        serde_json::from_slice(&std::fs::read(dir.join("instance.json")).map_err(|_| "not a Kromvia backup".to_string())?).map_err(|e| e.to_string())?;
    if inst.id != instance_id {
        return Err("backup belongs to another instance".into());
    }
    Ok(())
}

fn swap(target: &Path, new: &Path, old: &Path) -> Result<(), String> {
    if target.exists() {
        std::fs::rename(target, old).map_err(|e| e.to_string())?;
    }
    if let Err(e) = std::fs::rename(new, target) {
        let _ = std::fs::rename(old, target);
        return Err(e.to_string());
    }
    let _ = std::fs::remove_dir_all(old);
    Ok(())
}

pub fn delete(instance_id: &str, name: &str) -> Result<(), String> {
    std::fs::remove_file(file(instance_id, name)?).map_err(|e| e.to_string())
}

pub fn folder(instance_id: &str) -> Result<PathBuf, String> {
    let d = dir(instance_id)?;
    std::fs::create_dir_all(&d).map_err(|e| e.to_string())?;
    Ok(d)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn backs_up_and_restores() {
        paths::init(std::env::temp_dir().join(format!("kromvia-backup-{}", std::process::id())));
        let inst = instances::create("Backup", "1.21.4", None).unwrap();
        let game = instances::game_dir(&inst.id).unwrap();
        std::fs::create_dir_all(game.join("logs")).unwrap();
        std::fs::write(game.join("options.txt"), "fov:70").unwrap();
        std::fs::write(game.join("logs/latest.log"), "x").unwrap();

        let job = Job::detached("backup-test");
        let b = create(&inst.id, &job).unwrap();
        assert_eq!(list(&inst.id).unwrap()[0].name, b.name);

        std::fs::write(game.join("options.txt"), "fov:110").unwrap();
        std::fs::write(game.join("new.txt"), "y").unwrap();
        restore(&inst.id, &b.name).unwrap();
        assert_eq!(std::fs::read_to_string(game.join("options.txt")).unwrap(), "fov:70");
        assert!(!game.join("new.txt").exists());
        assert!(!game.join("logs/latest.log").exists());
        assert_eq!(instances::get(&inst.id).unwrap().name, "Backup");

        assert!(file(&inst.id, "../x.zip").is_err());
        delete(&inst.id, &b.name).unwrap();
        assert!(list(&inst.id).unwrap().is_empty());
    }
}
