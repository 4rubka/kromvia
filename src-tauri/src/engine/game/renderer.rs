
use std::sync::Arc;

use crate::engine::content::{self, modrinth, ContentKind};
use crate::engine::core::jobs::Job;
use crate::engine::game::loaders::LoaderKind;
use crate::engine::instances;

const VULKANMOD: &str = "JYQhtZtO";
const CONFLICTS: [&str; 4] = ["AANobbMI", "PtjYWJkn", "Bh37bMuy", "YL57xq9U"];
const RECORD: &str = "renderer-disabled.json";

fn record_path(instance_id: &str) -> Result<std::path::PathBuf, String> {
    Ok(instances::dir(instance_id)?.join(RECORD))
}

pub fn is_vulkan(instance_id: &str) -> Result<bool, String> {
    Ok(content::list(instance_id)?.iter().any(|f| f.enabled && f.project_id.as_deref() == Some(VULKANMOD)))
}

pub async fn set(instance_id: &str, vulkan: bool, job: &Arc<Job>) -> Result<(), String> {
    let inst = instances::get(instance_id)?;
    if !vulkan {
        for f in content::list(instance_id)?.iter().filter(|f| f.enabled && f.project_id.as_deref() == Some(VULKANMOD)) {
            content::set_enabled(instance_id, &f.file, false)?;
        }
        let path = record_path(instance_id)?;
        let disabled: Vec<String> = std::fs::read(&path).ok().and_then(|b| serde_json::from_slice(&b).ok()).unwrap_or_default();
        for file in disabled {
            let _ = content::set_enabled(instance_id, &file, true);
        }
        let _ = std::fs::remove_file(path);
        return Ok(());
    }
    if !matches!(inst.loader.as_ref().map(|l| l.kind), Some(LoaderKind::Fabric | LoaderKind::Quilt)) {
        return Err("VulkanMod needs a Fabric or Quilt build".into());
    }
    let files = content::list(instance_id)?;
    let mut disabled = vec![];
    for f in files.iter().filter(|f| f.enabled && f.project_id.as_deref().is_some_and(|p| CONFLICTS.contains(&p))) {
        content::set_enabled(instance_id, &f.file, false)?;
        disabled.push(f.file.clone());
    }
    let mut previous: Vec<String> = std::fs::read(record_path(instance_id)?).ok().and_then(|b| serde_json::from_slice(&b).ok()).unwrap_or_default();
    previous.extend(disabled.iter().cloned());
    std::fs::write(record_path(instance_id)?, serde_json::to_vec(&previous).map_err(|e| e.to_string())?).map_err(|e| e.to_string())?;
    match files.iter().find(|f| f.project_id.as_deref() == Some(VULKANMOD)) {
        Some(f) if !f.enabled => content::set_enabled(instance_id, &f.file, true),
        Some(_) => Ok(()),
        None => modrinth::install(instance_id, VULKANMOD, None, ContentKind::Mod, job).await.map(|_| ()),
    }
    .inspect_err(|_| {
        for file in &disabled {
            let _ = content::set_enabled(instance_id, file, true);
        }
    })
}
