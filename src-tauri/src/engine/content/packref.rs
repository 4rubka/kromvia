use std::path::PathBuf;

use serde::{Deserialize, Serialize};

use crate::engine::instances;

/// The modpack an instance was installed from, so a shared build can point at the pack instead of
/// re-listing its mods, and carry only what the player changed on top of it.
#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct PackRef {
    pub source: String,
    pub project: String,
    pub file: String,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct PackPath {
    pub path: String,
    pub sha1: String,
}

#[derive(Serialize, Deserialize)]
pub struct Saved {
    #[serde(flatten)]
    pub pack: PackRef,
    pub files: Vec<PackPath>,
}

fn path(instance_id: &str) -> Result<PathBuf, String> {
    Ok(instances::dir(instance_id)?.join("pack.json"))
}

pub fn save(instance_id: &str, pack: PackRef, files: Vec<PackPath>) -> Result<(), String> {
    let json = serde_json::to_vec_pretty(&Saved { pack, files }).map_err(|e| e.to_string())?;
    std::fs::write(path(instance_id)?, json).map_err(|e| e.to_string())
}

pub fn load(instance_id: &str) -> Option<Saved> {
    serde_json::from_slice(&std::fs::read(path(instance_id).ok()?).ok()?).ok()
}
