
use std::sync::Mutex;
use std::time::{Duration, Instant};

use serde::{Deserialize, Serialize};
use tauri::{PhysicalPosition, PhysicalSize, Runtime, WebviewWindow};

use crate::engine::core::paths;

#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq)]
struct State {
    x: i32,
    y: i32,
    width: u32,
    height: u32,
    maximized: bool,
}

fn path() -> std::path::PathBuf {
    paths::default_root().join("window.json")
}

pub fn restore<R: Runtime>(window: &WebviewWindow<R>) {
    let Some(s) = std::fs::read(path()).ok().and_then(|b| serde_json::from_slice::<State>(&b).ok()) else { return };
    let on_screen = window.available_monitors().unwrap_or_default().iter().any(|m| {
        let (p, z) = (m.position(), m.size());
        s.x + 40 >= p.x && s.y >= p.y && s.x + 40 < p.x + z.width as i32 && s.y + 40 < p.y + z.height as i32
    });
    if s.width >= 400 && s.height >= 300 {
        let _ = window.set_size(PhysicalSize::new(s.width, s.height));
    }
    if on_screen {
        let _ = window.set_position(PhysicalPosition::new(s.x, s.y));
    }
    if s.maximized {
        let _ = window.maximize();
    }
}

static LAST: Mutex<Option<Instant>> = Mutex::new(None);

pub fn save<R: Runtime>(window: &WebviewWindow<R>, force: bool) {
    {
        let mut last = LAST.lock().unwrap();
        if !force && last.is_some_and(|t| t.elapsed() < Duration::from_millis(500)) {
            return;
        }
        *last = Some(Instant::now());
    }
    if window.is_minimized().unwrap_or(false) {
        return;
    }
    let maximized = window.is_maximized().unwrap_or(false);
    let previous = std::fs::read(path()).ok().and_then(|b| serde_json::from_slice::<State>(&b).ok());
    let state = match (maximized, previous) {
        (true, Some(p)) => State { maximized: true, ..p },
        _ => {
            let (Ok(pos), Ok(size)) = (window.outer_position(), window.inner_size()) else { return };
            State { x: pos.x, y: pos.y, width: size.width, height: size.height, maximized }
        }
    };
    if let Ok(json) = serde_json::to_vec(&state) {
        let _ = std::fs::write(path(), json);
    }
}
