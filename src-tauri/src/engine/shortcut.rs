
use std::path::PathBuf;
#[cfg(not(target_os = "macos"))]
use std::process::Command;

use crate::engine::instances;

pub const ARG: &str = "--launch";

pub fn requested<I: IntoIterator<Item = String>>(args: I) -> Option<String> {
    let mut args = args.into_iter();
    args.find(|a| a == ARG)?;
    args.next()
}

fn exe() -> Result<PathBuf, String> {
    if let Some(app_image) = std::env::var_os("APPIMAGE") {
        return Ok(app_image.into());
    }
    std::env::current_exe().map_err(|e| e.to_string())
}

fn file_name(name: &str, id: &str) -> String {
    let s: String = name.chars().map(|c| if c.is_control() || r#"<>:"/\|?*"#.contains(c) { '_' } else { c }).collect();
    let s = s.trim().trim_matches('.').to_string();
    if s.is_empty() { id.to_string() } else { s }
}

pub fn create(instance_id: &str) -> Result<PathBuf, String> {
    let inst = instances::get(instance_id)?;
    if !inst.id.bytes().all(|b| b.is_ascii_alphanumeric() || b"-_.".contains(&b)) {
        return Err(format!("bad instance id {}", inst.id));
    }
    platform::create(&exe()?, &inst.id, &file_name(&inst.name, &inst.id), &inst.name)
}

#[cfg(target_os = "linux")]
mod platform {
    use super::*;
    use std::os::unix::fs::PermissionsExt;

    fn desktop() -> PathBuf {
        let from_xdg = Command::new("xdg-user-dir").arg("DESKTOP").output().ok().filter(|o| o.status.success());
        match from_xdg.map(|o| String::from_utf8_lossy(&o.stdout).trim().to_string()).filter(|s| !s.is_empty()) {
            Some(dir) => dir.into(),
            None => PathBuf::from(std::env::var_os("HOME").unwrap_or_default()).join("Desktop"),
        }
    }

    fn quote(arg: &str) -> String {
        let mut out = String::from("\"");
        for c in arg.chars() {
            if matches!(c, '"' | '`' | '$' | '\\') {
                out.push('\\');
            }
            out.push(c);
        }
        out.push('"');
        out.replace('%', "%%")
    }

    pub fn create(exe: &std::path::Path, id: &str, file: &str, name: &str) -> Result<PathBuf, String> {
        let dir = desktop();
        std::fs::create_dir_all(&dir).map_err(|e| e.to_string())?;
        let path = dir.join(format!("{file}.desktop"));
        let name = name.replace(['\n', '\r'], " ");
        let entry = format!(
            "[Desktop Entry]\nType=Application\nName={name}\nExec={} {ARG} {id}\nIcon=kromvia\nTerminal=false\nCategories=Game;\n",
            quote(&exe.to_string_lossy())
        );
        std::fs::write(&path, entry).map_err(|e| e.to_string())?;
        std::fs::set_permissions(&path, std::fs::Permissions::from_mode(0o755)).map_err(|e| e.to_string())?;
        let _ = Command::new("gio").arg("set").arg(&path).args(["metadata::trusted", "true"]).output();
        Ok(path)
    }
}

#[cfg(target_os = "windows")]
mod platform {
    use super::*;
    use std::os::windows::process::CommandExt;

    const CREATE_NO_WINDOW: u32 = 0x0800_0000;
    const SCRIPT: &str = "$d = [Environment]::GetFolderPath('Desktop'); $p = Join-Path $d ($env:K_FILE + '.lnk'); \
        $s = (New-Object -ComObject WScript.Shell).CreateShortcut($p); $s.TargetPath = $env:K_EXE; \
        $s.Arguments = '--launch ' + $env:K_ID; $s.IconLocation = $env:K_EXE; $s.Description = $env:K_NAME; $s.Save(); $p";

    pub fn create(exe: &std::path::Path, id: &str, file: &str, name: &str) -> Result<PathBuf, String> {
        let out = Command::new("powershell")
            .args(["-NoProfile", "-NonInteractive", "-Command", SCRIPT])
            .env("K_EXE", exe)
            .env("K_ID", id)
            .env("K_FILE", file)
            .env("K_NAME", name)
            .creation_flags(CREATE_NO_WINDOW)
            .output()
            .map_err(|e| e.to_string())?;
        if !out.status.success() {
            return Err(String::from_utf8_lossy(&out.stderr).trim().to_string());
        }
        Ok(String::from_utf8_lossy(&out.stdout).trim().into())
    }
}

#[cfg(target_os = "macos")]
mod platform {
    use super::*;
    use std::os::unix::fs::PermissionsExt;

    fn quote(arg: &str) -> String {
        format!("'{}'", arg.replace('\'', r"'\''"))
    }

    pub fn create(exe: &std::path::Path, id: &str, file: &str, _name: &str) -> Result<PathBuf, String> {
        let dir = PathBuf::from(std::env::var_os("HOME").unwrap_or_default()).join("Desktop");
        let path = dir.join(format!("{file}.command"));
        let script = format!("#!/bin/sh\n{} {ARG} {id} >/dev/null 2>&1 &\n", quote(&exe.to_string_lossy()));
        std::fs::write(&path, script).map_err(|e| e.to_string())?;
        std::fs::set_permissions(&path, std::fs::Permissions::from_mode(0o755)).map_err(|e| e.to_string())?;
        Ok(path)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn reads_the_request_and_names_files() {
        let args = |a: &[&str]| a.iter().map(|s| s.to_string()).collect::<Vec<_>>();
        assert_eq!(requested(args(&["kromvia", "--launch", "pvp"])), Some("pvp".into()));
        assert_eq!(requested(args(&["kromvia"])), None);
        assert_eq!(requested(args(&["kromvia", "--launch"])), None);
        assert_eq!(file_name("PvP: 1.21/test", "pvp"), "PvP_ 1.21_test");
        assert_eq!(file_name(" .. ", "pvp"), "pvp");
    }
}
