use tauri::{AppHandle, State};

use crate::engine::core::jobs::Jobs;
use crate::engine::java::{self, JavaRuntime};

#[tauri::command]
pub async fn java_list() -> Vec<JavaRuntime> {
    java::list().await
}

#[tauri::command]
pub fn java_supported() -> Vec<u32> {
    java::SUPPORTED_MAJORS.to_vec()
}

#[tauri::command]
pub fn java_install(app: AppHandle, jobs: State<'_, Jobs>, major: u32) -> Result<u64, String> {
    jobs.spawn(&app, format!("java-{major}"), move |job| java::install(major, job))
}

#[tauri::command]
pub fn java_remove(major: u32) -> Result<(), String> {
    java::remove(major)
}

#[tauri::command]
pub async fn java_check(path: String) -> Result<JavaRuntime, String> {
    java::check(&path).await
}
