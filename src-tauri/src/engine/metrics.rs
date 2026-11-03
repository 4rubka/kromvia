
use std::time::Duration;

use serde::{Deserialize, Serialize};
use tokio::sync::oneshot;

use crate::engine::{content, instances};

const KEEP: usize = 40;
const EVERY: Duration = Duration::from_secs(5);

#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct Run {
    pub started: u64,
    pub seconds: u64,
    pub ram_avg_mb: u64,
    pub ram_peak_mb: u64,
    pub cpu_avg: f32,
    pub mods: Vec<String>,
}

fn path(instance_id: &str) -> Result<std::path::PathBuf, String> {
    Ok(instances::dir(instance_id)?.join("metrics.json"))
}

pub fn list(instance_id: &str) -> Vec<Run> {
    path(instance_id).ok().and_then(|p| std::fs::read(p).ok()).and_then(|b| serde_json::from_slice(&b).ok()).unwrap_or_default()
}

fn save(instance_id: &str, run: Run) {
    let mut runs = list(instance_id);
    runs.push(run);
    let skip = runs.len().saturating_sub(KEEP);
    if let (Ok(p), Ok(json)) = (path(instance_id), serde_json::to_vec(&runs[skip..])) {
        let _ = std::fs::write(p, json);
    }
}

pub fn watch(instance_id: String, pid: u32) -> oneshot::Sender<()> {
    let (tx, mut rx) = oneshot::channel();
    let mods: Vec<String> = content::list(&instance_id).map(|l| l.into_iter().filter(|f| f.folder == "mods" && f.enabled).map(|f| f.title).collect()).unwrap_or_default();
    let started = instances::now();
    tauri::async_runtime::spawn(async move {
        use sysinfo::{Pid, ProcessRefreshKind, ProcessesToUpdate, System};
        let pid = Pid::from_u32(pid);
        let cores = std::thread::available_parallelism().map_or(1, |n| n.get()) as f32;
        let mut sys = System::new();
        let (mut ram, mut peak, mut cpu, mut n) = (0u64, 0u64, 0f32, 0u64);
        let begun = std::time::Instant::now();
        loop {
            tokio::select! {
                _ = &mut rx => break,
                _ = tokio::time::sleep(EVERY) => {}
            }
            sys.refresh_processes_specifics(ProcessesToUpdate::Some(&[pid]), true, ProcessRefreshKind::nothing().with_memory().with_cpu());
            let Some(p) = sys.process(pid) else { break };
            if begun.elapsed() < EVERY * 2 {
                continue;
            }
            let mb = p.memory() / (1024 * 1024);
            ram += mb;
            peak = peak.max(mb);
            cpu += p.cpu_usage() / cores;
            n += 1;
        }
        if n >= 2 {
            save(&instance_id, Run { started, seconds: begun.elapsed().as_secs(), ram_avg_mb: ram / n, ram_peak_mb: peak, cpu_avg: cpu / n as f32, mods });
        }
    });
    tx
}
