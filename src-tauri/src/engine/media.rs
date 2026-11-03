
use std::path::PathBuf;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use base64::Engine;
use serde::Serialize;
use tauri::{AppHandle, Emitter, Manager, Runtime};

use crate::engine::core::paths;
use crate::engine::game::launch::Games;

#[derive(Clone, Default, PartialEq)]
struct Now {
    player: String,
    title: String,
    artist: String,
    album: String,
    art_key: String,
    playing: bool,
    position_ms: u64,
    length_ms: u64,
}

#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct Track {
    player: String,
    title: String,
    artist: String,
    album: String,
    art: Option<String>,
    playing: bool,
    position_ms: u64,
    length_ms: u64,
    at: u64,
}

static LAST: std::sync::Mutex<Option<Track>> = std::sync::Mutex::new(None);

pub fn last() -> Option<Track> {
    LAST.lock().unwrap().clone()
}

fn epoch_ms() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_millis() as u64).unwrap_or(0)
}

fn art_dir() -> PathBuf {
    paths::temp().join("media")
}

async fn fetch_art(key: &str, smtc: Option<Vec<u8>>) -> Option<Vec<u8>> {
    if let Some(bytes) = smtc {
        return Some(bytes);
    }
    if let Some(path) = key.strip_prefix("file://") {
        let path = percent_decode(path);
        return tokio::fs::read(path).await.ok().filter(|b| b.len() <= MAX_ART);
    }
    if key.starts_with("https://") {
        let res = reqwest::get(key).await.ok()?.error_for_status().ok()?;
        let bytes = res.bytes().await.ok()?;
        return (bytes.len() <= MAX_ART).then(|| bytes.to_vec());
    }
    None
}

const MAX_ART: usize = 4 << 20;

fn percent_decode(s: &str) -> String {
    let b = s.as_bytes();
    let mut out = Vec::with_capacity(b.len());
    let mut i = 0;
    while i < b.len() {
        if b[i] == b'%' && i + 2 < b.len() {
            if let Some(v) = std::str::from_utf8(&b[i + 1..i + 3]).ok().and_then(|h| u8::from_str_radix(h, 16).ok()) {
                out.push(v);
                i += 3;
                continue;
            }
        }
        out.push(b[i]);
        i += 1;
    }
    String::from_utf8_lossy(&out).into_owned()
}

fn mime(bytes: &[u8]) -> &'static str {
    match bytes {
        [0x89, b'P', b'N', b'G', ..] => "image/png",
        [b'R', b'I', b'F', b'F', _, _, _, _, b'W', b'E', b'B', b'P', ..] => "image/webp",
        _ => "image/jpeg",
    }
}

pub fn start<R: Runtime>(app: &AppHandle<R>) {
    let app = app.clone();
    tauri::async_runtime::spawn(async move {
        let mut last: Option<Now> = None;
        let mut last_at = 0u64;
        let mut art: Option<(String, Option<String>, Option<PathBuf>)> = None;
        loop {
            let read = platform::now().await;
            let at = epoch_ms();
            let (now, thumb) = match read {
                Some((n, t)) => (Some(n), t),
                None => (None, None),
            };
            if changed(&last, last_at, &now, at) {
                if let Some(n) = &now {
                    if art.as_ref().map(|a| &a.0) != Some(&n.art_key) {
                        let bytes = if n.art_key.is_empty() { None } else { fetch_art(&n.art_key, thumb).await };
                        let path = bytes.as_ref().and_then(|b| save_art(&n.art_key, b));
                        let url = bytes.map(|b| format!("data:{};base64,{}", mime(&b), base64::engine::general_purpose::STANDARD.encode(&b)));
                        art = Some((n.art_key.clone(), url, path));
                    }
                }
                let (url, path) = art.as_ref().map(|a| (a.1.clone(), a.2.clone())).unwrap_or_default();
                let track = now.as_ref().map(|n| Track {
                    player: n.player.clone(),
                    title: n.title.clone(),
                    artist: n.artist.clone(),
                    album: n.album.clone(),
                    art: url,
                    playing: n.playing,
                    position_ms: n.position_ms,
                    length_ms: n.length_ms,
                    at,
                });
                let _ = app.emit("media", &track);
                *LAST.lock().unwrap() = track;
                let for_game = now.as_ref().map(|n| {
                    serde_json::json!({
                        "title": n.title, "artist": n.artist, "playing": n.playing,
                        "position": n.position_ms, "length": n.length_ms,
                        "art": path.as_ref().map(|p| p.to_string_lossy().into_owned()),
                    })
                });
                app.state::<Games>().media(for_game);
                last = now;
                last_at = at;
            }
            tokio::time::sleep(Duration::from_secs(1)).await;
        }
    });
}

fn changed(last: &Option<Now>, last_at: u64, now: &Option<Now>, at: u64) -> bool {
    let (Some(a), Some(b)) = (last, now) else {
        return last.is_some() != now.is_some();
    };
    let same = Now { position_ms: 0, ..a.clone() } == Now { position_ms: 0, ..b.clone() };
    if !same {
        return true;
    }
    let expected = if a.playing { a.position_ms + at.saturating_sub(last_at) } else { a.position_ms };
    expected.abs_diff(b.position_ms) > 2000
}

fn save_art(key: &str, bytes: &[u8]) -> Option<PathBuf> {
    use std::hash::{Hash, Hasher};
    let dir = art_dir();
    let _ = std::fs::remove_dir_all(&dir);
    std::fs::create_dir_all(&dir).ok()?;
    let mut h = std::collections::hash_map::DefaultHasher::new();
    key.hash(&mut h);
    let path = dir.join(format!("{:016x}", h.finish()));
    std::fs::write(&path, bytes).ok()?;
    Some(path)
}

pub async fn control(action: &str) -> Result<(), String> {
    match action {
        "play_pause" | "next" | "previous" => platform::control(action).await,
        _ => Err(format!("unknown action {action}")),
    }
}

#[cfg(target_os = "linux")]
mod platform {
    use std::collections::HashMap;

    use zbus::zvariant::{OwnedValue, Value};
    use zbus::{Connection, Proxy};

    use super::Now;

    const PREFIX: &str = "org.mpris.MediaPlayer2.";
    const PATH: &str = "/org/mpris/MediaPlayer2";
    const PLAYER: &str = "org.mpris.MediaPlayer2.Player";

    async fn proxy<'a>(conn: &Connection, name: String, iface: &'static str) -> zbus::Result<Proxy<'a>> {
        zbus::proxy::Builder::new(conn)
            .destination(name)?
            .path(PATH)?
            .interface(iface)?
            .cache_properties(zbus::proxy::CacheProperties::No)
            .build()
            .await
    }

    async fn pick(conn: &Connection) -> Option<(String, String)> {
        let names = zbus::fdo::DBusProxy::new(conn).await.ok()?.list_names().await.ok()?;
        let mut paused = None;
        for name in names.into_iter().map(|n| n.to_string()).filter(|n| n.starts_with(PREFIX)) {
            let Ok(p) = proxy(conn, name.clone(), PLAYER).await else { continue };
            match p.get_property::<String>("PlaybackStatus").await.as_deref() {
                Ok("Playing") => return Some((name, "Playing".into())),
                Ok("Paused") if paused.is_none() => paused = Some((name, "Paused".into())),
                _ => {}
            }
        }
        paused
    }

    fn inner<'a>(v: &'a Value<'a>) -> &'a Value<'a> {
        match v {
            Value::Value(b) => inner(b),
            v => v,
        }
    }

    fn text(meta: &HashMap<String, OwnedValue>, key: &str) -> String {
        meta.get(key).and_then(|v| inner(v).downcast_ref::<&str>().ok().map(String::from)).unwrap_or_default()
    }

    fn number(v: &Value) -> Option<u64> {
        match inner(v) {
            Value::I64(n) => u64::try_from(*n).ok(),
            Value::U64(n) => Some(*n),
            Value::I32(n) => u64::try_from(*n).ok(),
            Value::U32(n) => Some(u64::from(*n)),
            Value::F64(n) if *n >= 0.0 => Some(*n as u64),
            _ => None,
        }
    }

    fn artists(meta: &HashMap<String, OwnedValue>) -> String {
        let Some(v) = meta.get("xesam:artist") else { return String::new() };
        match inner(v) {
            Value::Array(a) => a.iter().filter_map(|x| inner(x).downcast_ref::<&str>().ok()).collect::<Vec<_>>().join(", "),
            Value::Str(s) => s.to_string(),
            _ => String::new(),
        }
    }

    pub async fn now() -> Option<(Now, Option<Vec<u8>>)> {
        let conn = Connection::session().await.ok()?;
        let (name, status) = pick(&conn).await?;
        let p = proxy(&conn, name.clone(), PLAYER).await.ok()?;
        let meta: HashMap<String, OwnedValue> = p.get_property::<OwnedValue>("Metadata").await.ok()?.try_into().ok()?;
        let title = text(&meta, "xesam:title");
        if title.is_empty() {
            return None;
        }
        let position = p.get_property::<i64>("Position").await.ok().and_then(|n| u64::try_from(n).ok()).unwrap_or(0);
        let identity = match proxy(&conn, name.clone(), "org.mpris.MediaPlayer2").await {
            Ok(root) => root.get_property::<String>("Identity").await.ok(),
            Err(_) => None,
        };
        let player = identity.unwrap_or_else(|| name.trim_start_matches(PREFIX).split('.').next().unwrap_or_default().to_string());
        let now = Now {
            player,
            title,
            artist: artists(&meta),
            album: text(&meta, "xesam:album"),
            art_key: text(&meta, "mpris:artUrl"),
            playing: status == "Playing",
            position_ms: position / 1000,
            length_ms: meta.get("mpris:length").and_then(|v| number(v)).unwrap_or(0) / 1000,
        };
        Some((now, None))
    }

    pub async fn control(action: &str) -> Result<(), String> {
        let conn = Connection::session().await.map_err(|e| e.to_string())?;
        let (name, _) = pick(&conn).await.ok_or("nothing is playing")?;
        let p = proxy(&conn, name, PLAYER).await.map_err(|e| e.to_string())?;
        let method = match action {
            "play_pause" => "PlayPause",
            "next" => "Next",
            _ => "Previous",
        };
        p.call_method(method, &()).await.map(|_| ()).map_err(|e| e.to_string())
    }
}

#[cfg(windows)]
mod platform {
    use windows::Media::Control::{
        GlobalSystemMediaTransportControlsSession as Session, GlobalSystemMediaTransportControlsSessionManager as Manager,
        GlobalSystemMediaTransportControlsSessionPlaybackStatus as Status,
    };
    use windows::Storage::Streams::DataReader;

    use super::Now;

    fn session() -> windows::core::Result<Session> {
        Manager::RequestAsync()?.join()?.GetCurrentSession()
    }

    fn thumbnail(props: &windows::Media::Control::GlobalSystemMediaTransportControlsSessionMediaProperties) -> windows::core::Result<Vec<u8>> {
        let stream = props.Thumbnail()?.OpenReadAsync()?.join()?;
        let size = stream.Size()? as u32;
        if size as usize > super::MAX_ART {
            return Ok(Vec::new());
        }
        let reader = DataReader::CreateDataReader(&stream)?;
        reader.LoadAsync(size)?.join()?;
        let mut buf = vec![0u8; size as usize];
        reader.ReadBytes(&mut buf)?;
        Ok(buf)
    }

    fn read() -> windows::core::Result<Option<(Now, Option<Vec<u8>>)>> {
        let s = session()?;
        let props = s.TryGetMediaPropertiesAsync()?.join()?;
        let title = props.Title()?.to_string();
        if title.is_empty() {
            return Ok(None);
        }
        let status = s.GetPlaybackInfo()?.PlaybackStatus()?;
        if status != Status::Playing && status != Status::Paused {
            return Ok(None);
        }
        let timeline = s.GetTimelineProperties()?;
        let ticks_ms = |t: i64| u64::try_from(t / 10_000).unwrap_or(0);
        let thumb = thumbnail(&props).ok().filter(|b| !b.is_empty());
        let artist = props.Artist()?.to_string();
        let album = props.AlbumTitle()?.to_string();
        let art_key = thumb.as_ref().map(|_| format!("smtc:{title}\u{0}{artist}\u{0}{album}")).unwrap_or_default();
        let now = Now {
            player: s.SourceAppUserModelId()?.to_string().trim_end_matches(".exe").to_string(),
            title,
            artist,
            album,
            art_key,
            playing: status == Status::Playing,
            position_ms: ticks_ms(timeline.Position()?.Duration),
            length_ms: ticks_ms(timeline.EndTime()?.Duration),
        };
        Ok(Some((now, thumb)))
    }

    pub async fn now() -> Option<(Now, Option<Vec<u8>>)> {
        tauri::async_runtime::spawn_blocking(|| read().ok().flatten()).await.ok().flatten()
    }

    pub async fn control(action: &str) -> Result<(), String> {
        let action = action.to_string();
        tauri::async_runtime::spawn_blocking(move || -> windows::core::Result<bool> {
            let s = session()?;
            match action.as_str() {
                "play_pause" => s.TryTogglePlayPauseAsync()?.join(),
                "next" => s.TrySkipNextAsync()?.join(),
                _ => s.TrySkipPreviousAsync()?.join(),
            }
        })
        .await
        .map_err(|e| e.to_string())?
        .map(|_| ())
        .map_err(|e| e.message().to_string())
    }
}

#[cfg(not(any(target_os = "linux", windows)))]
mod platform {
    use super::Now;

    pub async fn now() -> Option<(Now, Option<Vec<u8>>)> {
        None
    }

    pub async fn control(_action: &str) -> Result<(), String> {
        Err("not supported on this system".into())
    }
}
