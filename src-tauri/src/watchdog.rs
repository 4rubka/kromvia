
use std::path::PathBuf;
use std::sync::atomic::{AtomicBool, Ordering};
use std::time::Duration;

use tauri::{AppHandle, Manager};
use tauri_plugin_dialog::{DialogExt, MessageDialogKind};

use crate::engine::core::{crashlog, paths};

const WAIT: Duration = Duration::from_secs(25);
const RESTARTS: u32 = 2;

#[derive(Default)]
pub struct UiReady(AtomicBool);

fn tries_file() -> PathBuf {
    paths::root().join("ui-restarts")
}

fn tries() -> u32 {
    std::fs::read_to_string(tries_file()).ok().and_then(|s| s.trim().parse().ok()).unwrap_or(0)
}

pub fn ready(app: &AppHandle) {
    app.state::<UiReady>().0.store(true, Ordering::Relaxed);
    let _ = std::fs::remove_file(tries_file());
}

pub fn start(app: &AppHandle) {
    let app = app.clone();
    tauri::async_runtime::spawn(async move {
        let ready = || app.state::<UiReady>().0.load(Ordering::Relaxed);
        tokio::time::sleep(WAIT).await;
        if ready() {
            return;
        }
        crashlog::write("ui", "the page did not start, reloading");
        if let Some(w) = app.get_webview_window("main") {
            let _ = w.reload();
        }
        tokio::time::sleep(WAIT).await;
        if ready() {
            return;
        }
        let tries = tries() + 1;
        if tries <= RESTARTS {
            crashlog::write("ui", &format!("the page did not start after a reload, clearing the webview cache and restarting ({tries}/{RESTARTS})"));
            let _ = std::fs::write(tries_file(), tries.to_string());
            clear_cache(&app);
            app.restart();
        }
        let _ = std::fs::remove_file(tries_file());
        crashlog::write("ui", "the page still does not start, giving up");
        let log = crashlog::dir().join("launcher.log");
        app.dialog()
            .message(format!("Kromvia could not open its window. The details are in {}", log.display()))
            .title("Kromvia")
            .kind(MessageDialogKind::Error)
            .show(|_| {});
    });
}

fn clear_cache(app: &AppHandle) {
    let p = app.path();
    let data = ["WebKitCache", "CacheStorage", "EBWebView/Default/Cache", "EBWebView/Default/Code Cache", "EBWebView/Default/GPUCache"];
    for dir in [p.app_local_data_dir(), p.app_data_dir()].into_iter().flatten() {
        for rel in data {
            let _ = std::fs::remove_dir_all(dir.join(rel));
        }
    }
    if let Ok(dir) = p.app_cache_dir() {
        let _ = std::fs::remove_dir_all(dir.join("WebKit"));
    }
}
