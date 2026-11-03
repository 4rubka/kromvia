use tauri::State;

use crate::engine::core::jobs::{JobInfo, Jobs};

#[tauri::command]
pub fn jobs_list(jobs: State<'_, Jobs>) -> Vec<JobInfo> {
    jobs.list()
}

#[tauri::command]
pub fn job_cancel(jobs: State<'_, Jobs>, id: u64) -> bool {
    jobs.cancel(id)
}
