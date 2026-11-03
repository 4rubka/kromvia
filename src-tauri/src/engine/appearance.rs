
use std::io::Read;
use std::path::{Path, PathBuf};

use base64::Engine;
use serde::{Deserialize, Serialize};

use crate::engine::core::paths::{self, safe_join};

const MAX_CSS: usize = 512 * 1024;
const MAX_ZIP_IMAGES: usize = 8 * 1024 * 1024;
const MAX_BACKGROUND: usize = 4 * 1024 * 1024;
const JPEG: &str = "data:image/jpeg;base64,";

#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
pub struct Theme {
    pub id: String,
    pub name: String,
}

fn themes_dir() -> PathBuf {
    paths::default_root().join("themes")
}

fn theme_path(id: &str) -> Result<PathBuf, String> {
    if id.is_empty() || !id.bytes().all(|b| b.is_ascii_alphanumeric() || b == b'-') {
        return Err(format!("bad theme id {id}"));
    }
    safe_join(&themes_dir(), &format!("{id}.css"))
}

fn slug(name: &str) -> String {
    let s: String = name.chars().map(|c| if c.is_ascii_alphanumeric() { c.to_ascii_lowercase() } else { '-' }).collect();
    let s = s.split('-').filter(|p| !p.is_empty()).collect::<Vec<_>>().join("-");
    if s.is_empty() { "theme".into() } else { s.chars().take(48).collect() }
}

pub fn themes() -> Vec<Theme> {
    let Ok(rd) = std::fs::read_dir(themes_dir()) else { return vec![] };
    let mut out: Vec<Theme> = rd
        .flatten()
        .filter_map(|e| {
            let name = e.file_name().to_string_lossy().into_owned();
            let id = name.strip_suffix(".css")?.to_string();
            let css = std::fs::read_to_string(e.path()).ok()?;
            let title = css.lines().next().and_then(|l| l.trim().strip_prefix("/* name:")).and_then(|l| l.strip_suffix("*/")).map(|s| s.trim().to_string());
            Some(Theme { name: title.unwrap_or_else(|| id.clone()), id })
        })
        .collect();
    out.sort_by_key(|t| t.name.to_lowercase());
    out
}

pub fn theme_css(id: &str) -> Result<String, String> {
    std::fs::read_to_string(theme_path(id)?).map_err(|e| e.to_string())
}

pub fn delete_theme(id: &str) -> Result<(), String> {
    std::fs::remove_file(theme_path(id)?).map_err(|e| e.to_string())
}

fn image_type(name: &str) -> Option<&'static str> {
    let lower = name.to_ascii_lowercase();
    [("png", "image/png"), ("jpg", "image/jpeg"), ("jpeg", "image/jpeg"), ("webp", "image/webp"), ("gif", "image/gif")]
        .into_iter()
        .find(|(ext, _)| lower.ends_with(&format!(".{ext}")))
        .map(|(_, t)| t)
}

fn inline_images(css: &str, images: &[(String, String)]) -> String {
    let mut out = css.to_string();
    for (name, data) in images {
        let base = name.rsplit('/').next().unwrap_or(name);
        for target in [name.as_str(), base] {
            for q in ["", "'", "\""] {
                out = out.replace(&format!("url({q}{target}{q})"), &format!("url(\"{data}\")"));
                out = out.replace(&format!("url({q}./{target}{q})"), &format!("url(\"{data}\")"));
            }
        }
    }
    out
}

fn read_zip(path: &Path) -> Result<String, String> {
    let file = std::fs::File::open(path).map_err(|e| e.to_string())?;
    let mut zip = zip::ZipArchive::new(file).map_err(|e| e.to_string())?;
    let mut css = None;
    let mut images = vec![];
    let mut total = 0usize;
    for i in 0..zip.len() {
        let mut entry = zip.by_index(i).map_err(|e| e.to_string())?;
        let Ok(name) = entry.name().map(|n| n.to_string()) else { continue };
        if entry.is_dir() || name.contains("..") {
            continue;
        }
        if name.rsplit('/').next() == Some("theme.css") && css.is_none() {
            if entry.size() as usize > MAX_CSS {
                return Err("theme.css is too big".into());
            }
            let mut s = String::new();
            entry.read_to_string(&mut s).map_err(|e| e.to_string())?;
            css = Some(s);
        } else if let Some(mime) = image_type(&name) {
            total += entry.size() as usize;
            if total > MAX_ZIP_IMAGES {
                return Err("the theme's pictures are too big".into());
            }
            let mut b = vec![];
            entry.read_to_end(&mut b).map_err(|e| e.to_string())?;
            images.push((name, format!("data:{mime};base64,{}", base64::engine::general_purpose::STANDARD.encode(b))));
        }
    }
    let css = css.ok_or("the zip has no theme.css")?;
    Ok(inline_images(&css, &images))
}

fn save_theme(name: &str, css: &str) -> Result<Theme, String> {
    let name: String = name.trim().chars().take(48).collect();
    std::fs::create_dir_all(themes_dir()).map_err(|e| e.to_string())?;
    let base = slug(&name);
    let mut id = base.clone();
    let mut n = 2;
    while theme_path(&id)?.exists() {
        id = format!("{base}-{n}");
        n += 1;
    }
    let body = css.lines().next().filter(|l| l.trim().starts_with("/* name:")).map_or(css.to_string(), |first| css[first.len()..].trim_start().to_string());
    std::fs::write(theme_path(&id)?, format!("/* name: {name} */\n{body}")).map_err(|e| e.to_string())?;
    Ok(Theme { id, name })
}

pub fn import_theme(path: &str) -> Result<Theme, String> {
    let path = Path::new(path);
    let name = path.file_stem().and_then(|s| s.to_str()).unwrap_or("Theme").to_string();
    let css = match path.extension().and_then(|e| e.to_str()).map(str::to_ascii_lowercase).as_deref() {
        Some("zip") => read_zip(path)?,
        Some("css") => {
            if std::fs::metadata(path).map_err(|e| e.to_string())?.len() as usize > MAX_CSS {
                return Err("theme file is too big".into());
            }
            std::fs::read_to_string(path).map_err(|e| e.to_string())?
        }
        _ => return Err("themes are .css or .zip files".into()),
    };
    save_theme(&name, &css)
}

fn background_path() -> PathBuf {
    paths::default_root().join("background.jpg")
}

pub fn background() -> Option<String> {
    std::fs::read(background_path()).ok().map(|b| format!("{JPEG}{}", base64::engine::general_purpose::STANDARD.encode(b)))
}

pub fn set_background(data_url: Option<&str>) -> Result<(), String> {
    let Some(data_url) = data_url else {
        return match std::fs::remove_file(background_path()) {
            Err(e) if e.kind() != std::io::ErrorKind::NotFound => Err(e.to_string()),
            _ => Ok(()),
        };
    };
    let b64 = data_url.strip_prefix(JPEG).ok_or("background must be a JPEG")?;
    if b64.len() > MAX_BACKGROUND * 4 / 3 + 4 {
        return Err("background is too big".into());
    }
    let jpeg = base64::engine::general_purpose::STANDARD.decode(b64).map_err(|e| e.to_string())?;
    if !jpeg.starts_with(&[0xFF, 0xD8, 0xFF]) {
        return Err("background must be a JPEG".into());
    }
    std::fs::create_dir_all(paths::default_root()).map_err(|e| e.to_string())?;
    std::fs::write(background_path(), jpeg).map_err(|e| e.to_string())
}

#[derive(Serialize, Deserialize, Debug)]
pub struct Backup {
    pub kromvia_settings: u32,
    pub local: serde_json::Map<String, serde_json::Value>,
    #[serde(default)]
    pub themes: Vec<(String, String)>,
    #[serde(default)]
    pub background: Option<String>,
}

pub fn export(path: &str, local: serde_json::Map<String, serde_json::Value>) -> Result<(), String> {
    let themes = themes().into_iter().filter_map(|t| theme_css(&t.id).ok().map(|css| (t.name, css))).collect();
    let backup = Backup { kromvia_settings: 1, local, themes, background: background() };
    std::fs::write(path, serde_json::to_vec_pretty(&backup).map_err(|e| e.to_string())?).map_err(|e| e.to_string())
}

pub fn import(path: &str) -> Result<serde_json::Map<String, serde_json::Value>, String> {
    if std::fs::metadata(path).map_err(|e| e.to_string())?.len() > 32 * 1024 * 1024 {
        return Err("backup file is too big".into());
    }
    let backup: Backup = serde_json::from_slice(&std::fs::read(path).map_err(|e| e.to_string())?).map_err(|_| "not a Kromvia settings backup".to_string())?;
    if backup.kromvia_settings != 1 {
        return Err("backup is from a newer Kromvia".into());
    }
    let have: Vec<String> = themes().into_iter().map(|t| t.name).collect();
    for (name, css) in backup.themes.iter().filter(|(n, _)| !have.contains(n)) {
        if css.len() <= MAX_CSS + MAX_ZIP_IMAGES * 2 {
            save_theme(name, css)?;
        }
    }
    if let Some(bg) = &backup.background {
        set_background(Some(bg))?;
    }
    Ok(backup.local.into_iter().filter(|(k, v)| k.starts_with("kromvia.") && v.is_string()).collect())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn inlines_zip_pictures() {
        let css = ".a{background:url(bg.png)} .b{background:url('img/x.jpg')} .c{background:url(https://e.com/y.png)}";
        let out = inline_images(css, &[("bg.png".into(), "data:image/png;base64,AA".into()), ("img/x.jpg".into(), "data:image/jpeg;base64,BB".into())]);
        assert!(out.contains(r#"url("data:image/png;base64,AA")"#));
        assert!(out.contains(r#"url("data:image/jpeg;base64,BB")"#));
        assert!(out.contains("url(https://e.com/y.png)"));
    }

    #[test]
    fn imports_and_backs_up_themes() {
        paths::init(std::env::temp_dir().join(format!("kromvia-theme-{}", std::process::id())));
        let dir = paths::default_root().join("in");
        std::fs::create_dir_all(&dir).unwrap();
        std::fs::write(dir.join("Night Blue.css"), ":root{--accent:#00f}").unwrap();
        let t = import_theme(dir.join("Night Blue.css").to_str().unwrap()).unwrap();
        assert_eq!(t, Theme { id: "night-blue".into(), name: "Night Blue".into() });
        assert!(themes().contains(&t));
        assert!(theme_css("night-blue").unwrap().ends_with(":root{--accent:#00f}"));
        assert!(theme_path("../x").is_err());
        assert!(import_theme(dir.join("nope.txt").to_str().unwrap()).is_err());

        let mut local = serde_json::Map::new();
        local.insert("kromvia.theme".into(), "light".into());
        let file = dir.join("backup.json");
        export(file.to_str().unwrap(), local).unwrap();
        delete_theme("night-blue").unwrap();
        let back = import(file.to_str().unwrap()).unwrap();
        assert_eq!(back.get("kromvia.theme").and_then(|v| v.as_str()), Some("light"));
        assert!(themes().iter().any(|t| t.name == "Night Blue"));
        for t in themes() {
            delete_theme(&t.id).unwrap();
        }
    }
}
