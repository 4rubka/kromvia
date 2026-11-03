
use std::path::PathBuf;
use std::sync::Mutex;
use std::time::{SystemTime, UNIX_EPOCH};

use base64::Engine;
use serde::{Deserialize, Serialize};
use sha1::{Digest, Sha1};

use crate::engine::core::http;
use crate::engine::core::paths;

const MAX_PNG: usize = 64 * 1024;
const MAX_CAPE: usize = 512 * 1024;

#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct LibrarySkin {
    pub id: String,
    pub name: String,
    pub slim: bool,
    pub added: u64,
}

#[derive(Serialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct SkinView {
    #[serde(flatten)]
    pub skin: LibrarySkin,
    pub data_url: String,
}

#[derive(Serialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct FoundSkin {
    pub name: String,
    pub slim: bool,
    pub data_url: String,
    pub cape_url: Option<String>,
}

static LOCK: Mutex<()> = Mutex::new(());

fn dir() -> PathBuf {
    paths::root().join("skins")
}

fn index_path() -> PathBuf {
    dir().join("library.json")
}

fn read_index() -> Vec<LibrarySkin> {
    std::fs::read(index_path()).ok().and_then(|b| serde_json::from_slice(&b).ok()).unwrap_or_default()
}

fn write_index(entries: &[LibrarySkin]) -> Result<(), String> {
    std::fs::create_dir_all(dir()).map_err(|e| e.to_string())?;
    std::fs::write(index_path(), serde_json::to_vec_pretty(entries).map_err(|e| e.to_string())?).map_err(|e| e.to_string())
}

fn png_path(id: &str) -> Result<PathBuf, String> {
    if id.len() != 40 || !id.bytes().all(|b| b.is_ascii_hexdigit()) {
        return Err(format!("bad skin id {id}"));
    }
    Ok(dir().join(format!("{id}.png")))
}

fn data_url(png: &[u8]) -> String {
    format!("data:image/png;base64,{}", base64::engine::general_purpose::STANDARD.encode(png))
}

pub fn check_png(bytes: &[u8]) -> Result<(), String> {
    const SIG: &[u8] = b"\x89PNG\r\n\x1a\n";
    if bytes.len() < 24 || &bytes[..8] != SIG || &bytes[12..16] != b"IHDR" {
        return Err("skin must be a PNG file".into());
    }
    let w = u32::from_be_bytes(bytes[16..20].try_into().unwrap());
    let h = u32::from_be_bytes(bytes[20..24].try_into().unwrap());
    if w != 64 || (h != 64 && h != 32) {
        return Err(format!("skin must be 64×64 or 64×32, this one is {w}×{h}"));
    }
    Ok(())
}

fn now() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_secs()).unwrap_or(0)
}

pub fn add(png: &[u8], name: &str, slim: bool) -> Result<LibrarySkin, String> {
    check_png(png)?;
    let name: String = name.trim().chars().take(32).collect();
    let id: String = Sha1::digest(png).iter().map(|b| format!("{b:02x}")).collect();
    let _guard = LOCK.lock().unwrap();
    std::fs::create_dir_all(dir()).map_err(|e| e.to_string())?;
    std::fs::write(png_path(&id)?, png).map_err(|e| e.to_string())?;
    let mut index = read_index();
    index.retain(|s| s.id != id);
    let skin = LibrarySkin { id, name: if name.is_empty() { "Skin".into() } else { name }, slim, added: now() };
    index.insert(0, skin.clone());
    write_index(&index)?;
    Ok(skin)
}

pub fn list() -> Vec<SkinView> {
    read_index()
        .into_iter()
        .filter_map(|skin| {
            let png = std::fs::read(png_path(&skin.id).ok()?).ok()?;
            Some(SkinView { data_url: data_url(&png), skin })
        })
        .collect()
}

pub fn get(id: &str) -> Result<(LibrarySkin, Vec<u8>), String> {
    let skin = read_index().into_iter().find(|s| s.id == id).ok_or("skin is not in the library")?;
    let png = std::fs::read(png_path(id)?).map_err(|e| e.to_string())?;
    Ok((skin, png))
}

pub fn remove(id: &str) -> Result<(), String> {
    let _guard = LOCK.lock().unwrap();
    let mut index = read_index();
    index.retain(|s| s.id != id);
    write_index(&index)?;
    let _ = std::fs::remove_file(png_path(id)?);
    Ok(())
}

pub fn rename(id: &str, name: &str) -> Result<(), String> {
    let name: String = name.trim().chars().take(32).collect();
    if name.is_empty() {
        return Err("name is empty".into());
    }
    let _guard = LOCK.lock().unwrap();
    let mut index = read_index();
    index.iter_mut().find(|s| s.id == id).ok_or("skin is not in the library")?.name = name;
    write_index(&index)
}

pub async fn download_texture(url: &str) -> Result<Vec<u8>, String> {
    let url = url.replacen("http://", "https://", 1);
    if !url.starts_with("https://textures.minecraft.net/texture/") {
        return Err(format!("not a Minecraft texture: {url}"));
    }
    let resp = http::client().get(&url).send().await.map_err(|e| format!("{url}: {e}"))?;
    if !resp.status().is_success() {
        return Err(format!("{url}: HTTP {}", resp.status()));
    }
    let bytes = resp.bytes().await.map_err(|e| format!("{url}: {e}"))?;
    if bytes.len() > MAX_PNG {
        return Err("texture is too big".into());
    }
    Ok(bytes.to_vec())
}

#[derive(Deserialize)]
struct NameLookup {
    id: String,
    name: String,
}

#[derive(Deserialize)]
struct SessionProfile {
    properties: Vec<Property>,
}

#[derive(Deserialize)]
struct Property {
    name: String,
    value: String,
}

#[derive(Deserialize)]
struct Textures {
    textures: TextureSet,
}

#[derive(Deserialize)]
#[serde(rename_all = "UPPERCASE")]
struct TextureSet {
    skin: Option<Texture>,
    cape: Option<Texture>,
}

#[derive(Deserialize)]
struct Texture {
    url: String,
    #[serde(default)]
    metadata: Option<TextureMeta>,
}

#[derive(Deserialize)]
struct TextureMeta {
    model: String,
}

fn check_nick(nick: &str) -> Result<&str, String> {
    let nick = nick.trim();
    let ok = (1..=16).contains(&nick.len()) && nick.bytes().all(|b| b.is_ascii_alphanumeric() || b == b'_');
    if ok { Ok(nick) } else { Err("nicknames are 1–16 letters, digits or _".into()) }
}

fn parse_textures(value: &str) -> Result<(Option<String>, bool, Option<String>), String> {
    let json = base64::engine::general_purpose::STANDARD.decode(value).map_err(|e| e.to_string())?;
    let t: Textures = serde_json::from_slice(&json).map_err(|e| e.to_string())?;
    let slim = t.textures.skin.as_ref().and_then(|s| s.metadata.as_ref()).is_some_and(|m| m.model == "slim");
    Ok((t.textures.skin.map(|s| s.url), slim, t.textures.cape.map(|c| c.url)))
}

pub async fn lookup(nick: &str) -> Result<FoundSkin, String> {
    let nick = check_nick(nick)?;
    let url = format!("https://api.mojang.com/users/profiles/minecraft/{nick}");
    let resp = http::client().get(&url).send().await.map_err(|e| format!("{url}: {e}"))?;
    match resp.status().as_u16() {
        200 => {}
        204 | 404 => return Err(format!("no Minecraft player named {nick}")),
        s => return Err(format!("{url}: HTTP {s}")),
    }
    let found: NameLookup = resp.json().await.map_err(|e| format!("{url}: {e}"))?;
    if found.id.len() != 32 || !found.id.bytes().all(|b| b.is_ascii_hexdigit()) {
        return Err("Mojang returned a bad id".into());
    }
    let profile: SessionProfile = http::get_json(&format!("https://sessionserver.mojang.com/session/minecraft/profile/{}", found.id)).await?;
    let textures = profile.properties.iter().find(|p| p.name == "textures").ok_or("player has no textures")?;
    let (skin, slim, cape) = parse_textures(&textures.value)?;
    let skin = skin.ok_or_else(|| format!("{} uses a default skin", found.name))?;
    let png = download_texture(&skin).await?;
    check_png(&png)?;
    Ok(FoundSkin {
        name: found.name,
        slim,
        data_url: data_url(&png),
        cape_url: cape.map(|c| c.replacen("http://", "https://", 1)),
    })
}

pub async fn optifine_cape(nick: &str) -> Result<Option<String>, String> {
    let nick = check_nick(nick)?;
    let url = format!("https://optifine.net/capes/{nick}.png");
    let resp = http::client().get(&url).send().await.map_err(|e| format!("{url}: {e}"))?;
    match resp.status().as_u16() {
        200 => {}
        404 => return Ok(None),
        s => return Err(format!("{url}: HTTP {s}")),
    }
    let bytes = resp.bytes().await.map_err(|e| format!("{url}: {e}"))?;
    if bytes.len() > MAX_CAPE || !bytes.starts_with(b"\x89PNG\r\n\x1a\n") {
        return Ok(None);
    }
    Ok(Some(data_url(&bytes)))
}

pub fn add_from_data_url(data_url: &str, name: &str, slim: bool) -> Result<LibrarySkin, String> {
    let b64 = data_url.strip_prefix("data:image/png;base64,").ok_or("skin must be a PNG")?;
    if b64.len() > MAX_PNG * 2 {
        return Err("skin is too big".into());
    }
    let png = base64::engine::general_purpose::STANDARD.decode(b64).map_err(|e| e.to_string())?;
    add(&png, name, slim)
}

pub async fn add_from_nick(nick: &str) -> Result<LibrarySkin, String> {
    let found = lookup(nick).await?;
    let b64 = found.data_url.trim_start_matches("data:image/png;base64,");
    let png = base64::engine::general_purpose::STANDARD.decode(b64).map_err(|e| e.to_string())?;
    add(&png, &found.name, found.slim)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    #[ignore]
    async fn finds_optifine_capes() {
        assert!(optifine_cape("zzzNoSuchPlayer9").await.unwrap().is_none());
        assert!(optifine_cape("sp614x").await.unwrap().is_some_and(|c| c.starts_with("data:image/png")));
    }

    fn png(w: u32, h: u32) -> Vec<u8> {
        let mut b = b"\x89PNG\r\n\x1a\n\0\0\0\rIHDR".to_vec();
        b.extend(w.to_be_bytes());
        b.extend(h.to_be_bytes());
        b
    }

    #[test]
    fn rejects_bad_data_urls() {
        assert!(add_from_data_url("data:image/jpeg;base64,AAAA", "x", false).is_err());
        assert!(add_from_data_url("data:image/png;base64,!!!", "x", false).is_err());
        let huge = format!("data:image/png;base64,{}", "A".repeat(MAX_PNG * 3));
        assert!(add_from_data_url(&huge, "x", false).is_err());
    }

    #[test]
    fn checks_skin_files() {
        assert!(check_png(&png(64, 64)).is_ok());
        assert!(check_png(&png(64, 32)).is_ok());
        assert!(check_png(&png(128, 128)).is_err());
        assert!(check_png(b"GIF89a").is_err());
    }

    #[test]
    fn parses_textures_property() {
        let json = r#"{"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/abc","metadata":{"model":"slim"}},"CAPE":{"url":"http://textures.minecraft.net/texture/cape"}}}"#;
        let value = base64::engine::general_purpose::STANDARD.encode(json);
        let (skin, slim, cape) = parse_textures(&value).unwrap();
        assert_eq!(skin.as_deref(), Some("http://textures.minecraft.net/texture/abc"));
        assert!(slim);
        assert!(cape.is_some());
        let (_, slim, cape) = parse_textures(&base64::engine::general_purpose::STANDARD.encode(r#"{"textures":{"SKIN":{"url":"x"}}}"#)).unwrap();
        assert!(!slim && cape.is_none());
    }

    #[test]
    fn rejects_bad_input() {
        assert!(check_nick("Notch").is_ok());
        assert!(check_nick("a/b").is_err());
        assert!(check_nick("").is_err());
        assert!(png_path("../x").is_err());
    }

    #[test]
    fn library_dedupes_and_orders() {
        paths::init(std::env::temp_dir().join(format!("kromvia-skins-{}", std::process::id())));
        let a = add(&png(64, 64), "A", false).unwrap();
        let b = add(&png(64, 32), "B", true).unwrap();
        let again = add(&png(64, 64), "A2", false).unwrap();
        assert_eq!(a.id, again.id);
        let names: Vec<String> = list().into_iter().filter(|s| [&a.id, &b.id].contains(&&s.skin.id)).map(|s| s.skin.name).collect();
        assert_eq!(names, ["A2", "B"]);
        rename(&b.id, "Bee").unwrap();
        assert_eq!(get(&b.id).unwrap().0.name, "Bee");
        remove(&a.id).unwrap();
        remove(&b.id).unwrap();
        assert!(get(&a.id).is_err());
    }

    #[tokio::test]
    #[ignore]
    async fn looks_up_real_nick() {
        let found = lookup("jeb_").await.unwrap();
        println!("{} slim={} cape={:?} png={} bytes", found.name, found.slim, found.cape_url, found.data_url.len());
        assert_eq!(found.name, "jeb_");
        assert!(lookup("this_nick_is_way_too_long").await.is_err());
    }
}
