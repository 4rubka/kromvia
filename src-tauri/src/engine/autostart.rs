
use std::path::PathBuf;

#[cfg(not(target_os = "macos"))]
const NAME: &str = "Kromvia";

fn exe() -> Result<PathBuf, String> {
    std::env::current_exe().map_err(|e| e.to_string())
}

#[cfg(any(target_os = "linux", target_os = "macos"))]
fn home() -> PathBuf {
    std::env::var_os("HOME").or_else(|| std::env::var_os("USERPROFILE")).map(PathBuf::from).unwrap_or_default()
}

#[cfg(target_os = "linux")]
fn entry() -> PathBuf {
    std::env::var_os("XDG_CONFIG_HOME").map(PathBuf::from).unwrap_or_else(|| home().join(".config")).join("autostart/kromvia.desktop")
}

#[cfg(target_os = "macos")]
fn entry() -> PathBuf {
    home().join("Library/LaunchAgents/com.kromvia.launcher.plist")
}

#[cfg(target_os = "linux")]
fn contents() -> Result<String, String> {
    let exe = exe()?;
    let exe = std::env::var_os("APPIMAGE").map(PathBuf::from).unwrap_or(exe);
    let quoted = exe.to_string_lossy().replace('\\', "\\\\").replace('"', "\\\"");
    Ok(format!("[Desktop Entry]\nType=Application\nName={NAME}\nExec=\"{quoted}\"\nX-GNOME-Autostart-enabled=true\n"))
}

#[cfg(target_os = "macos")]
fn contents() -> Result<String, String> {
    let exe = exe()?.to_string_lossy().replace('&', "&amp;").replace('<', "&lt;").replace('>', "&gt;");
    Ok(format!(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n\
         <plist version=\"1.0\"><dict><key>Label</key><string>com.kromvia.launcher</string>\
         <key>ProgramArguments</key><array><string>{exe}</string></array><key>RunAtLoad</key><true/></dict></plist>\n"
    ))
}

#[cfg(any(target_os = "linux", target_os = "macos"))]
pub fn enabled() -> bool {
    entry().is_file()
}

#[cfg(any(target_os = "linux", target_os = "macos"))]
pub fn set(on: bool) -> Result<(), String> {
    let path = entry();
    if !on {
        return match std::fs::remove_file(&path) {
            Err(e) if e.kind() != std::io::ErrorKind::NotFound => Err(e.to_string()),
            _ => Ok(()),
        };
    }
    std::fs::create_dir_all(path.parent().unwrap()).map_err(|e| e.to_string())?;
    std::fs::write(path, contents()?).map_err(|e| e.to_string())
}

#[cfg(windows)]
const RUN_KEY: &str = r"HKCU\Software\Microsoft\Windows\CurrentVersion\Run";

#[cfg(windows)]
fn reg(args: &[&std::ffi::OsStr]) -> std::io::Result<std::process::Output> {
    use std::os::windows::process::CommandExt;
    std::process::Command::new("reg").args(args).creation_flags(0x0800_0000).output()
}

#[cfg(windows)]
pub fn enabled() -> bool {
    reg(&["query".as_ref(), RUN_KEY.as_ref(), "/v".as_ref(), NAME.as_ref()]).is_ok_and(|o| o.status.success())
}

#[cfg(windows)]
pub fn set(on: bool) -> Result<(), String> {
    let out = if on {
        let value = format!("\"{}\"", exe()?.display());
        reg(&["add".as_ref(), RUN_KEY.as_ref(), "/v".as_ref(), NAME.as_ref(), "/t".as_ref(), "REG_SZ".as_ref(), "/d".as_ref(), value.as_ref(), "/f".as_ref()])
    } else {
        if !enabled() {
            return Ok(());
        }
        reg(&["delete".as_ref(), RUN_KEY.as_ref(), "/v".as_ref(), NAME.as_ref(), "/f".as_ref()])
    }
    .map_err(|e| e.to_string())?;
    if out.status.success() { Ok(()) } else { Err(String::from_utf8_lossy(&out.stderr).trim().to_string()) }
}

#[cfg(test)]
mod tests {
    #[cfg(target_os = "linux")]
    #[test]
    fn writes_and_removes_linux_entry() {
        let dir = std::env::temp_dir().join(format!("kromvia-autostart-{}", std::process::id()));
        unsafe { std::env::set_var("XDG_CONFIG_HOME", &dir) };
        super::set(true).unwrap();
        assert!(super::enabled());
        let text = std::fs::read_to_string(super::entry()).unwrap();
        assert!(text.starts_with("[Desktop Entry]\n") && text.contains("Exec=\""), "{text}");
        super::set(false).unwrap();
        super::set(false).unwrap();
        assert!(!super::enabled());
        unsafe { std::env::remove_var("XDG_CONFIG_HOME") };
        let _ = std::fs::remove_dir_all(dir);
    }
}
