use tauri::{AppHandle, State};

use crate::engine::core::jobs::Jobs;
use crate::engine::game::launch::Games;
use crate::engine::core::paths;
use crate::engine::relocate;
use crate::engine::storage::{self, Usage};

#[tauri::command]
pub async fn storage_usage() -> Result<Usage, String> {
    tokio::task::spawn_blocking(storage::usage).await.map_err(|e| e.to_string())
}

#[tauri::command]
pub fn storage_clear_temp(jobs: State<'_, Jobs>) -> Result<u64, String> {
    if !jobs.list().is_empty() {
        return Err("wait for downloads to finish".into());
    }
    storage::clear_temp()
}

#[tauri::command]
pub fn storage_dedup(app: AppHandle, jobs: State<'_, Jobs>, games: State<'_, Games>) -> Result<u64, String> {
    if !games.list().is_empty() {
        return Err("close the game first".into());
    }
    jobs.spawn(&app, "dedup".into(), |job| async move { storage::dedup(&job).await.map(|_| ()) })
}

#[derive(serde::Serialize)]
#[serde(rename_all = "camelCase")]
pub struct Location {
    pub path: String,
    pub moved: bool,
}

#[tauri::command]
pub fn storage_location() -> Location {
    Location { path: paths::root().to_string_lossy().into_owned(), moved: paths::root() != paths::default_root() }
}

#[tauri::command]
pub fn storage_move(app: AppHandle, jobs: State<'_, Jobs>, games: State<'_, Games>, folder: String) -> Result<u64, String> {
    if !games.list().is_empty() {
        return Err("close the game first".into());
    }
    if !jobs.list().is_empty() {
        return Err("wait for downloads to finish".into());
    }
    let target = relocate::target_for(std::path::Path::new(&folder))?;
    jobs.spawn(&app.clone(), "move".into(), move |job| async move {
        let to = target.clone();
        tokio::task::spawn_blocking(move || relocate::relocate(paths::root(), &to, &job)).await.map_err(|e| e.to_string())??;
        paths::set_location(&target).map_err(|e| e.to_string())?;
        app.restart()
    })
}
