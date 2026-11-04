
use std::path::PathBuf;

use serde::Serialize;
use tauri::{AppHandle, State};

use crate::engine::core::jobs::Jobs;
use crate::engine::game::launch::{self, Games};
use crate::engine::instances::{self, Stats, Usage};
use crate::engine::servers::{self, SavedServer};
use crate::engine::worlds::{self, Screenshot, World};

fn not_running(games: &Games, instance_id: &str) -> Result<(), String> {
    if games.list().iter().any(|r| r.instance_id == instance_id) {
        return Err("stop the game first".into());
    }
    Ok(())
}

#[tauri::command]
pub fn world_list(instance_id: String) -> Result<Vec<World>, String> {
    worlds::list(&instance_id)
}

#[tauri::command]
pub fn world_rename(games: State<'_, Games>, instance_id: String, folder: String, name: String) -> Result<(), String> {
    not_running(&games, &instance_id)?;
    worlds::rename(&instance_id, &folder, &name)
}

#[tauri::command]
pub async fn world_duplicate(instance_id: String, folder: String) -> Result<String, String> {
    tauri::async_runtime::spawn_blocking(move || worlds::duplicate(&instance_id, &folder)).await.map_err(|e| e.to_string())?
}

#[tauri::command]
pub fn world_delete(games: State<'_, Games>, instance_id: String, folder: String) -> Result<(), String> {
    not_running(&games, &instance_id)?;
    worlds::delete(&instance_id, &folder)
}

#[tauri::command]
pub async fn world_export(instance_id: String, folder: String, path: PathBuf) -> Result<(), String> {
    tauri::async_runtime::spawn_blocking(move || worlds::export(&instance_id, &folder, &path)).await.map_err(|e| e.to_string())?
}

#[tauri::command]
pub fn saved_servers(instance_id: String) -> Result<Vec<SavedServer>, String> {
    servers::saved(&instance_id)
}

#[tauri::command]
pub fn saved_server_add(games: State<'_, Games>, instance_id: String, name: String, address: String) -> Result<(), String> {
    not_running(&games, &instance_id)?;
    if !launch::valid_address(&address) {
        return Err("bad server address".into());
    }
    servers::add_saved(&instance_id, &name, &address)
}

#[tauri::command]
pub fn saved_server_remove(games: State<'_, Games>, instance_id: String, address: String) -> Result<(), String> {
    not_running(&games, &instance_id)?;
    servers::remove_saved(&instance_id, &address)
}

#[tauri::command]
pub fn saved_server_pin(games: State<'_, Games>, instance_id: String, address: String, pinned: bool) -> Result<(), String> {
    not_running(&games, &instance_id)?;
    servers::set_pinned(&instance_id, &address, pinned)
}

#[tauri::command]
pub fn screenshot_list(instance_id: String) -> Result<Vec<Screenshot>, String> {
    worlds::list_screenshots(&instance_id)
}

#[tauri::command]
pub fn screenshot_delete(instance_id: String, name: String) -> Result<(), String> {
    std::fs::remove_file(worlds::screenshot_path(&instance_id, &name)?).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn screenshot_open(instance_id: String, name: String) -> Result<(), String> {
    tauri_plugin_opener::open_path(worlds::screenshot_path(&instance_id, &name)?, None::<&str>).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn screenshot_discord(instance_id: String, name: String) -> Result<(), String> {
    let path = worlds::screenshot_path(&instance_id, &name)?;
    tauri::async_runtime::spawn_blocking(move || crate::engine::discord::screenshot_to_discord(&path)).await.map_err(|e| e.to_string())?
}

const FOLDERS: [&str; 9] = ["", "saves", "screenshots", "mods", "resourcepacks", "shaderpacks", "config", "logs", "crash-reports"];

#[tauri::command]
pub fn instance_open_folder(instance_id: String, folder: String) -> Result<(), String> {
    if !FOLDERS.contains(&folder.as_str()) {
        return Err(format!("bad folder {folder}"));
    }
    let mut dir = instances::game_dir(&instance_id)?;
    if !folder.is_empty() {
        dir = dir.join(folder);
    }
    std::fs::create_dir_all(&dir).map_err(|e| e.to_string())?;
    tauri_plugin_opener::open_path(dir, None::<&str>).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn crash_report_open(instance_id: String, path: PathBuf) -> Result<(), String> {
    let dir = instances::game_dir(&instance_id)?.join("crash-reports");
    let path = path.canonicalize().map_err(|e| e.to_string())?;
    if !path.starts_with(dir.canonicalize().map_err(|e| e.to_string())?) {
        return Err("not a crash report of this instance".into());
    }
    tauri_plugin_opener::open_path(path, None::<&str>).map_err(|e| e.to_string())
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct InstanceInfo {
    stats: Stats,
    usage: Usage,
}

#[derive(serde::Serialize)]
#[serde(rename_all = "camelCase")]
pub struct InstanceSummary {
    last_played: u64,
    mods: usize,
}

#[tauri::command]
pub fn instance_summary(id: String) -> Result<InstanceSummary, String> {
    let mods = std::fs::read_dir(instances::game_dir(&id)?.join("mods"))
        .map(|d| d.flatten().filter(|e| e.file_name().to_string_lossy().ends_with(".jar")).count())
        .unwrap_or(0);
    Ok(InstanceSummary { last_played: instances::stats(&id).last_played, mods })
}

#[tauri::command]
pub fn instance_open_log(instance_id: String) -> Result<(), String> {
    let log = instances::game_dir(&instance_id)?.join("logs").join("latest.log");
    if !log.is_file() {
        return Err("no latest.log yet".into());
    }
    tauri_plugin_opener::open_path(log, None::<&str>).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn instance_info(id: String) -> Result<InstanceInfo, String> {
    tauri::async_runtime::spawn_blocking(move || Ok(InstanceInfo { stats: instances::stats(&id), usage: instances::usage(&id)? }))
        .await
        .map_err(|e| e.to_string())?
}

#[tauri::command]
pub fn instance_clean(games: State<'_, Games>, id: String) -> Result<u64, String> {
    not_running(&games, &id)?;
    instances::clean(&id)
}

#[tauri::command]
pub fn instance_repair(app: AppHandle, jobs: State<'_, Jobs>, id: String) -> Result<u64, String> {
    let inst = instances::get(&id)?;
    jobs.spawn(&app, format!("repair-{id}"), move |job| async move {
        let broken = launch::repair(&inst, &job).await?;
        if broken == 0 { Ok(()) } else { Err(format!("repaired {broken}")) }
    })
}
