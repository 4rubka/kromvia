use tauri::{AppHandle, State};

use crate::engine::import;
use crate::engine::core::jobs::Jobs;
use crate::engine::game::install::{self, ManifestEntry};
use crate::engine::game::loaders::{self, Loader, LoaderKind, LoaderVersion};
use crate::engine::game::presets::{self, Preset};
use crate::engine::game::launch::{self, Games, LaunchOptions, RunInfo};
use crate::engine::instances::{self, Instance, Settings};

#[tauri::command]
pub async fn mc_versions() -> Result<Vec<ManifestEntry>, String> {
    Ok(install::manifest().await?.versions)
}

#[tauri::command]
pub async fn loader_versions(kind: LoaderKind, mc: String) -> Result<Vec<LoaderVersion>, String> {
    loaders::versions(kind, &mc).await
}

#[tauri::command]
pub fn instance_list() -> Vec<Instance> {
    instances::list()
}

#[tauri::command]
pub fn instance_create(name: String, mc_version: String, loader: Option<Loader>) -> Result<Instance, String> {
    instances::create(&name, &mc_version, loader)
}

#[tauri::command]
pub fn instance_metrics(id: String) -> Vec<crate::engine::metrics::Run> {
    crate::engine::metrics::list(&id)
}

#[tauri::command]
pub fn instance_update(id: String, settings: Settings) -> Result<Instance, String> {
    instances::update(&id, settings)
}

#[tauri::command]
pub fn preset_apply(app: AppHandle, jobs: State<'_, Jobs>, instance_id: String, preset: Preset) -> Result<u64, String> {
    jobs.spawn(&app, format!("preset-{instance_id}"), move |job| async move {
        let skipped = presets::apply(&instance_id, preset, &job).await?;
        if skipped.is_empty() { Ok(()) } else { Err(format!("applied, but skipped {}", skipped.join("; "))) }
    })
}

#[tauri::command]
pub fn renderer_get(instance_id: String) -> Result<bool, String> {
    crate::engine::game::renderer::is_vulkan(&instance_id)
}

#[tauri::command]
pub fn renderer_set(app: AppHandle, jobs: State<'_, Jobs>, instance_id: String, vulkan: bool) -> Result<u64, String> {
    jobs.spawn(&app, format!("renderer-{instance_id}"), move |job| async move { crate::engine::game::renderer::set(&instance_id, vulkan, &job).await })
}

#[tauri::command]
pub fn template_list() -> Vec<crate::engine::templates::Template> {
    crate::engine::templates::list()
}

#[tauri::command]
pub fn template_save(instance_id: String, name: String) -> Result<crate::engine::templates::Template, String> {
    crate::engine::templates::save(&instance_id, &name)
}

#[tauri::command]
pub fn template_delete(id: String) -> Result<(), String> {
    crate::engine::templates::delete(&id)
}

#[tauri::command]
pub fn template_create(template_id: String, name: String) -> Result<Instance, String> {
    crate::engine::templates::create(&template_id, &name)
}

#[tauri::command]
pub fn instance_duplicate(id: String) -> Result<Instance, String> {
    instances::duplicate(&id)
}

#[tauri::command]
pub fn client_versions() -> Vec<String> {
    crate::engine::core::paths::client_versions()
}

#[tauri::command]
pub fn instance_cover(id: String) -> Result<Option<String>, String> {
    instances::cover(&id)
}

#[tauri::command]
pub fn instance_set_cover(id: String, cover: Option<String>) -> Result<(), String> {
    instances::set_cover(&id, cover.as_deref())
}

#[tauri::command]
pub fn instance_set_icon(id: String, icon: Option<String>) -> Result<Instance, String> {
    instances::set_icon(&id, icon)
}

#[tauri::command]
pub fn instance_delete(games: State<'_, Games>, id: String) -> Result<(), String> {
    if games.list().iter().any(|r| r.instance_id == id) {
        return Err("stop the game first".into());
    }
    instances::delete(&id)
}

#[tauri::command]
#[allow(clippy::too_many_arguments)]
pub fn game_launch(
    app: AppHandle,
    jobs: State<'_, Jobs>,
    instance_id: String,
    username: String,
    account_id: Option<String>,
    join: Option<String>,
    world: Option<String>,
    room: Option<bool>,
) -> Result<u64, String> {
    launch::start(&app, &jobs, instance_id, username, account_id, LaunchOptions { join, world, room: room.unwrap_or(false) })
}

#[tauri::command]
pub fn game_running(games: State<'_, Games>) -> Vec<RunInfo> {
    games.list()
}

#[tauri::command]
pub fn game_stop(games: State<'_, Games>, run_id: u64) -> bool {
    games.stop(run_id)
}

#[tauri::command]
pub fn game_toast(games: State<'_, Games>, title: String, text: String) {
    games.toast(&title, &text);
}

#[tauri::command]
pub fn game_social(games: State<'_, Games>, state: serde_json::Value) {
    games.social(&state);
}

#[tauri::command]
pub fn import_list() -> Vec<import::Found> {
    import::list_all()
}

#[tauri::command]
pub fn import_scan(path: String) -> Vec<import::Found> {
    import::scan(std::path::Path::new(&path))
}

#[tauri::command]
pub fn import_instance(app: AppHandle, jobs: State<'_, Jobs>, path: String) -> Result<u64, String> {
    jobs.spawn(&app, format!("import-{path}"), move |job| async move { import::import(&path, &job).await.map(|_| ()) })
}

#[tauri::command]
pub fn crash_reports_get() -> bool {
    crate::engine::game::report::enabled()
}

#[tauri::command]
pub fn crash_reports_set(on: bool) -> Result<(), String> {
    crate::engine::game::report::set_enabled(on)
}

#[tauri::command]
pub async fn crash_report_send(reports: State<'_, crate::engine::game::report::Reports>, instance_id: String) -> Result<(), String> {
    let report = reports.take(&instance_id).ok_or("this crash report is gone, restart the game to get a new one")?;
    crate::engine::game::report::send(&report).await
}
