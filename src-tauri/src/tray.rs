
use tauri::menu::{Menu, MenuItem};
use tauri::tray::{MouseButton, MouseButtonState, TrayIcon, TrayIconBuilder, TrayIconEvent};
use tauri::{AppHandle, Manager, Wry};

use crate::engine::core::crashlog;

pub struct Tray(Option<(TrayIcon, MenuItem<Wry>, MenuItem<Wry>)>);

impl Tray {
    pub fn available(&self) -> bool {
        self.0.is_some()
    }

    pub fn set_labels(&self, show: &str, quit: &str) {
        if let Some((_, s, q)) = &self.0 {
            let _ = s.set_text(show);
            let _ = q.set_text(quit);
        }
    }
}

pub fn show_main(app: &AppHandle) {
    if let Some(w) = app.get_webview_window("main") {
        let _ = w.unminimize();
        let _ = w.show();
        let _ = w.set_focus();
    }
}

pub fn init(app: &AppHandle) -> Tray {
    match build(app) {
        Ok(parts) => Tray(Some(parts)),
        Err(e) => {
            crashlog::write("tray", &e.to_string());
            Tray(None)
        }
    }
}

fn build(app: &AppHandle) -> tauri::Result<(TrayIcon, MenuItem<Wry>, MenuItem<Wry>)> {
    let show = MenuItem::with_id(app, "show", "Show Kromvia", true, None::<&str>)?;
    let quit = MenuItem::with_id(app, "quit", "Quit", true, None::<&str>)?;
    let mut builder = TrayIconBuilder::with_id("main")
        .tooltip("Kromvia")
        .menu(&Menu::with_items(app, &[&show, &quit])?)
        .show_menu_on_left_click(false)
        .on_menu_event(|app, e| match e.id().as_ref() {
            "show" => show_main(app),
            "quit" => app.exit(0),
            _ => {}
        })
        .on_tray_icon_event(|tray, e| {
            if let TrayIconEvent::Click { button: MouseButton::Left, button_state: MouseButtonState::Up, .. } = e {
                show_main(tray.app_handle());
            }
        });
    if let Some(icon) = app.default_window_icon() {
        builder = builder.icon(icon.clone());
    }
    Ok((builder.build(app)?, show, quit))
}
