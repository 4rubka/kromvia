use std::path::Path;

pub fn open_path(path: impl AsRef<Path>) -> Result<(), String> {
    let path = path.as_ref();
    #[cfg(target_os = "linux")]
    if let Ok(appdir) = std::env::var("APPDIR") {
        path.metadata().map_err(|e| e.to_string())?;
        let mut cmd = std::process::Command::new("xdg-open");
        cmd.arg(path).env_remove("APPDIR").env_remove("APPIMAGE");
        for (key, value) in std::env::vars() {
            if !value.contains(&appdir) {
                continue;
            }
            let kept = value.split(':').filter(|p| !p.is_empty() && !p.starts_with(&appdir)).collect::<Vec<_>>().join(":");
            if kept.is_empty() {
                cmd.env_remove(key);
            } else {
                cmd.env(key, kept);
            }
        }
        let mut child = cmd.spawn().map_err(|e| e.to_string())?;
        std::thread::spawn(move || child.wait());
        return Ok(());
    }
    tauri_plugin_opener::open_path(path, None::<&str>).map_err(|e| e.to_string())
}
