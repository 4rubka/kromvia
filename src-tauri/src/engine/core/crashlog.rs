
use std::io::Write;
use std::path::PathBuf;

use crate::engine::core::paths;
use crate::engine::instances::now;

const MAX_LOG: u64 = 1024 * 1024;

pub fn dir() -> PathBuf {
    paths::root().join("logs")
}

pub fn write(source: &str, text: &str) {
    let dir = dir();
    let path = dir.join("launcher.log");
    let _ = std::fs::create_dir_all(&dir);
    if std::fs::metadata(&path).is_ok_and(|m| m.len() > MAX_LOG) {
        let _ = std::fs::rename(&path, dir.join("launcher.log.old"));
    }
    if let Ok(mut f) = std::fs::OpenOptions::new().create(true).append(true).open(&path) {
        let _ = writeln!(f, "[{}] {source} v{}: {text}", now(), env!("CARGO_PKG_VERSION"));
    }
}

pub fn install() {
    let default = std::panic::take_hook();
    std::panic::set_hook(Box::new(move |info| {
        write("panic", &format!("{info}\n{}", std::backtrace::Backtrace::force_capture()));
        default(info);
    }));
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn appends_entries() {
        paths::init(std::env::temp_dir().join(format!("kromvia-crashlog-{}", std::process::id())));
        write("webview", "TypeError: x is undefined");
        write("webview", "second");
        let log = std::fs::read_to_string(dir().join("launcher.log")).unwrap();
        assert!(log.contains("webview v") && log.contains("TypeError") && log.ends_with("second\n"));
    }
}
