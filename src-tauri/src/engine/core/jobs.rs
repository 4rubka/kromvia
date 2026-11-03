use std::collections::HashMap;
use std::future::Future;
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant};

use serde::Serialize;
use tauri::{AppHandle, Emitter, Manager, Runtime};

const PROGRESS_INTERVAL: Duration = Duration::from_millis(100);

pub struct Job {
    pub id: u64,
    pub key: String,
    cancel: AtomicBool,
    last_emit: Mutex<Option<Instant>>,
    emit_progress: Box<dyn Fn(ProgressEvent) + Send + Sync>,
}

#[derive(Serialize, Clone)]
pub struct JobInfo {
    pub id: u64,
    pub key: String,
}

#[derive(Serialize, Clone)]
struct ProgressEvent<'a> {
    id: u64,
    key: &'a str,
    stage: &'a str,
    done: u64,
    total: Option<u64>,
}

#[derive(Serialize, Clone)]
struct FinishedEvent {
    id: u64,
    key: String,
    error: Option<String>,
}

impl Job {
    pub fn detached(key: &str) -> Job {
        Job { id: 0, key: key.into(), cancel: AtomicBool::new(false), last_emit: Mutex::new(None), emit_progress: Box::new(|_| {}) }
    }

    pub fn cancel_flag(&self) -> &AtomicBool {
        &self.cancel
    }

    pub fn is_cancelled(&self) -> bool {
        self.cancel.load(Ordering::Relaxed)
    }

    pub fn progress(&self, stage: &str, done: u64, total: Option<u64>) {
        let now = Instant::now();
        {
            let mut last = self.last_emit.lock().unwrap();
            let finished = total == Some(done);
            if !finished && last.is_some_and(|t| now - t < PROGRESS_INTERVAL) {
                return;
            }
            *last = Some(now);
        }
        (self.emit_progress)(ProgressEvent { id: self.id, key: &self.key, stage, done, total });
    }
}

#[derive(Default)]
pub struct Jobs {
    running: Mutex<HashMap<u64, Arc<Job>>>,
}

impl Jobs {
    pub fn spawn<R: Runtime, F, Fut>(&self, app: &AppHandle<R>, key: String, task: F) -> Result<u64, String>
    where
        F: FnOnce(Arc<Job>) -> Fut + Send + 'static,
        Fut: Future<Output = Result<(), String>> + Send + 'static,
    {
        static NEXT_ID: AtomicU64 = AtomicU64::new(1);

        let job = {
            let mut running = self.running.lock().unwrap();
            if running.values().any(|j| j.key == key) {
                return Err(format!("{key} is already in progress"));
            }
            let job = Arc::new(Job {
                id: NEXT_ID.fetch_add(1, Ordering::Relaxed),
                key,
                cancel: AtomicBool::new(false),
                last_emit: Mutex::new(None),
                emit_progress: {
                    let app = app.clone();
                    Box::new(move |e| {
                        let _ = app.emit("job-progress", e);
                    })
                },
            });
            running.insert(job.id, job.clone());
            job
        };

        let id = job.id;
        let app = app.clone();
        tauri::async_runtime::spawn(async move {
            let result = task(job.clone()).await;
            app.state::<Jobs>().running.lock().unwrap().remove(&id);
            let error = match result {
                Ok(()) => None,
                Err(_) if job.is_cancelled() => Some("cancelled".to_string()),
                Err(e) => Some(e),
            };
            let _ = app.emit("job-finished", FinishedEvent { id, key: job.key.clone(), error });
        });
        Ok(id)
    }

    pub fn cancel(&self, id: u64) -> bool {
        match self.running.lock().unwrap().get(&id) {
            Some(job) => {
                job.cancel.store(true, Ordering::Relaxed);
                true
            }
            None => false,
        }
    }

    pub fn list(&self) -> Vec<JobInfo> {
        self.running
            .lock()
            .unwrap()
            .values()
            .map(|j| JobInfo { id: j.id, key: j.key.clone() })
            .collect()
    }
}
