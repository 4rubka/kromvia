use std::collections::HashMap;
use std::sync::Mutex;

use serde_json::{json, Value};

use super::crash::Crash;
use crate::engine::core::{http, paths};
use crate::engine::instances;

const MAX_LOG: usize = 96 * 1024;
const MAX_REPORT: usize = 64 * 1024;
const MAX_MODS: usize = 400;

#[derive(Default)]
pub struct Reports(Mutex<HashMap<String, Value>>);

impl Reports {
    pub fn keep(&self, instance_id: &str, report: Value) {
        self.0.lock().unwrap().insert(instance_id.to_string(), report);
    }

    pub fn take(&self, instance_id: &str) -> Option<Value> {
        self.0.lock().unwrap().remove(instance_id)
    }
}

fn flag() -> std::path::PathBuf {
    paths::root().join("crash-reports")
}

pub fn enabled() -> bool {
    std::fs::read_to_string(flag()).is_ok_and(|s| s.trim() == "on")
}

pub fn set_enabled(on: bool) -> Result<(), String> {
    std::fs::create_dir_all(paths::root()).map_err(|e| e.to_string())?;
    std::fs::write(flag(), if on { "on" } else { "off" }).map_err(|e| e.to_string())
}

fn tail(s: &str, max: usize) -> &str {
    if s.len() <= max {
        return s;
    }
    let mut i = s.len() - max;
    while !s.is_char_boundary(i) {
        i += 1;
    }
    &s[i..]
}

fn redact_tokens(s: &str) -> String {
    let mut out = String::with_capacity(s.len());
    let mut rest = s;
    while let Some(i) = rest.find("eyJ") {
        out.push_str(&rest[..i]);
        let len = rest[i..].find(|c: char| !(c.is_ascii_alphanumeric() || "-_.".contains(c))).unwrap_or(rest.len() - i);
        out.push_str(if len > 40 { "<token>" } else { &rest[i..i + len] });
        rest = &rest[i + len..];
    }
    out.push_str(rest);
    out
}

pub fn scrub(text: &str, player: &str, uuid: Option<&str>) -> String {
    let mut s = redact_tokens(text);
    let home = std::env::var("HOME").or_else(|_| std::env::var("USERPROFILE")).unwrap_or_default();
    if home.len() > 3 {
        s = s.replace(&home, "~").replace(&home.replace('\\', "/"), "~");
    }
    if let Some(u) = uuid.filter(|u| !u.is_empty()) {
        s = s.replace(u, "<uuid>").replace(&u.replace('-', ""), "<uuid>");
    }
    if player.len() >= 3 {
        s = s.replace(player, "<player>");
    }
    s
}

pub fn build(instance_id: &str, crash: &Crash, log: &str, report: &str, player: &str, uuid: Option<&str>) -> Value {
    let inst = instances::get(instance_id).ok();
    let mods: Vec<String> = crate::engine::content::list(instance_id)
        .unwrap_or_default()
        .into_iter()
        .filter(|f| f.folder == "mods" && f.enabled)
        .map(|f| f.file)
        .take(MAX_MODS)
        .collect();
    json!({
        "launcher": env!("CARGO_PKG_VERSION"),
        "os": std::env::consts::OS,
        "arch": std::env::consts::ARCH,
        "mc": inst.as_ref().map(|i| i.mc_version.clone()),
        "loader": inst.as_ref().and_then(|i| i.loader.as_ref()).map(|l| json!({ "kind": l.kind, "version": l.version })),
        "cause": crash.cause,
        "detail": crash.detail.as_deref().map(|d| scrub(d, player, uuid)),
        "mods": mods,
        "log": scrub(tail(log, MAX_LOG), player, uuid),
        "report": scrub(tail(report, MAX_REPORT), player, uuid),
    })
}

pub async fn send(report: &Value) -> Result<(), String> {
    let resp = http::client()
        .post(format!("{}/crashes", crate::engine::social::base()))
        .json(report)
        .send()
        .await
        .map_err(|e| format!("Kromvia servers are unreachable: {e}"))?;
    if resp.status().is_success() {
        Ok(())
    } else {
        Err(format!("crash report rejected ({})", resp.status()))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn scrubs_personal_data() {
        let token = format!("eyJ{}", "a".repeat(60));
        let text = format!("Setting user: Steve\n--accessToken {token} uuid 0123abcd-0000-0000-0000-000000000000");
        let out = scrub(&text, "Steve", Some("0123abcd-0000-0000-0000-000000000000"));
        assert_eq!(out, "Setting user: <player>\n--accessToken <token> uuid <uuid>");
    }

    #[test]
    fn tail_keeps_char_boundary() {
        assert_eq!(tail("aéb", 2), "b");
    }
}
