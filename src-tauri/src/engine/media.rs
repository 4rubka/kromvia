
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
    player_id: String,
    shuffle: Option<bool>,
    repeat: Option<&'static str>,
    players: Vec<Source>,
}

#[derive(Clone, Default, PartialEq, Serialize)]
pub struct Source {
    id: String,
    name: String,
    playing: bool,
}

static PREFERRED: std::sync::Mutex<Option<String>> = std::sync::Mutex::new(None);

fn preferred() -> Option<String> {
    PREFERRED.lock().unwrap().clone()
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
    loudness::start(app);
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
                        "album": n.album, "player": n.player, "playerId": n.player_id,
                        "shuffle": n.shuffle, "repeat": n.repeat, "players": n.players,
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

pub enum Action {
    PlayPause,
    Next,
    Previous,
    Seek(u64),
    Shuffle(bool),
    Repeat(&'static str),
}

pub async fn control(action: &str, value: &serde_json::Value) -> Result<(), String> {
    let action = match action {
        "play_pause" => Action::PlayPause,
        "next" => Action::Next,
        "previous" => Action::Previous,
        "seek" => Action::Seek(value.as_u64().ok_or("seek needs a position")?),
        "shuffle" => Action::Shuffle(value.as_bool().ok_or("shuffle needs on or off")?),
        "repeat" => Action::Repeat(match value.as_str() {
            Some("track") => "track",
            Some("playlist") => "playlist",
            _ => "none",
        }),
        "select" => {
            *PREFERRED.lock().unwrap() = value.as_str().map(String::from);
            return Ok(());
        }
        _ => return Err(format!("unknown action {action}")),
    };
    platform::control(action).await
}

#[cfg(target_os = "linux")]
mod platform {
    use std::collections::HashMap;

    use zbus::zvariant::{OwnedValue, Value};
    use zbus::{Connection, Proxy};

    use super::{Action, Now, Source};

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

    async fn players(conn: &Connection) -> Vec<(String, String)> {
        let Ok(dbus) = zbus::fdo::DBusProxy::new(conn).await else { return vec![] };
        let Ok(names) = dbus.list_names().await else { return vec![] };
        let mut out = vec![];
        for name in names.into_iter().map(|n| n.to_string()).filter(|n| n.starts_with(PREFIX)) {
            let Ok(p) = proxy(conn, name.clone(), PLAYER).await else { continue };
            if let Ok(status) = p.get_property::<String>("PlaybackStatus").await {
                out.push((name, status));
            }
        }
        out
    }

    async fn pick(conn: &Connection) -> Option<(String, String)> {
        let all = players(conn).await;
        if let Some(want) = super::preferred() {
            if let Some(p) = all.iter().find(|(n, _)| *n == want) {
                return Some(p.clone());
            }
        }
        all.iter().find(|(_, s)| s == "Playing").or_else(|| all.iter().find(|(_, s)| s == "Paused")).cloned()
    }

    async fn identity(conn: &Connection, name: &str) -> String {
        let id = match proxy(conn, name.to_string(), "org.mpris.MediaPlayer2").await {
            Ok(root) => root.get_property::<String>("Identity").await.ok(),
            Err(_) => None,
        };
        id.unwrap_or_else(|| name.trim_start_matches(PREFIX).split('.').next().unwrap_or_default().to_string())
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
        let player = identity(&conn, &name).await;
        let mut sources = vec![];
        for (n, status) in players(&conn).await {
            sources.push(Source { name: identity(&conn, &n).await, id: n, playing: status == "Playing" });
        }
        let repeat = match p.get_property::<String>("LoopStatus").await.as_deref() {
            Ok("Track") => Some("track"),
            Ok("Playlist") => Some("playlist"),
            Ok(_) => Some("none"),
            Err(_) => None,
        };
        let now = Now {
            player_id: name.clone(),
            shuffle: p.get_property::<bool>("Shuffle").await.ok(),
            repeat,
            players: sources,
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

    pub async fn control(action: Action) -> Result<(), String> {
        let conn = Connection::session().await.map_err(|e| e.to_string())?;
        let (name, _) = pick(&conn).await.ok_or("nothing is playing")?;
        let p = proxy(&conn, name, PLAYER).await.map_err(|e| e.to_string())?;
        let done = match action {
            Action::PlayPause => p.call_method("PlayPause", &()).await.map(|_| ()),
            Action::Next => p.call_method("Next", &()).await.map(|_| ()),
            Action::Previous => p.call_method("Previous", &()).await.map(|_| ()),
            Action::Seek(ms) => {
                let now = p.get_property::<i64>("Position").await.unwrap_or(0);
                p.call_method("Seek", &(ms as i64 * 1000 - now)).await.map(|_| ())
            }
            Action::Shuffle(on) => p.set_property("Shuffle", on).await.map_err(zbus::Error::from),
            Action::Repeat(mode) => {
                let status = match mode {
                    "track" => "Track",
                    "playlist" => "Playlist",
                    _ => "None",
                };
                p.set_property("LoopStatus", status).await.map_err(zbus::Error::from)
            }
        };
        done.map_err(|e| e.to_string())
    }
}

#[cfg(windows)]
mod platform {
    use windows::Media::Control::{
        GlobalSystemMediaTransportControlsSession as Session, GlobalSystemMediaTransportControlsSessionManager as Manager,
        GlobalSystemMediaTransportControlsSessionPlaybackStatus as Status,
    };
    use windows::Media::MediaPlaybackAutoRepeatMode as RepeatMode;
    use windows::Storage::Streams::DataReader;

    use super::{Action, Now, Source};

    fn sessions() -> windows::core::Result<Vec<Session>> {
        Ok(Manager::RequestAsync()?.join()?.GetSessions()?.into_iter().collect())
    }

    fn session() -> windows::core::Result<Session> {
        if let Some(want) = super::preferred() {
            for s in sessions()? {
                if s.SourceAppUserModelId()?.to_string() == want {
                    return Ok(s);
                }
            }
        }
        Manager::RequestAsync()?.join()?.GetCurrentSession()
    }

    fn app_name(id: &str) -> String {
        let base = id.rsplit(['\\', '!']).next().unwrap_or(id);
        base.trim_end_matches(".exe").to_string()
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
        let info = s.GetPlaybackInfo()?;
        let status = info.PlaybackStatus()?;
        if status != Status::Playing && status != Status::Paused {
            return Ok(None);
        }
        let timeline = s.GetTimelineProperties()?;
        let ticks_ms = |t: i64| u64::try_from(t / 10_000).unwrap_or(0);
        let thumb = thumbnail(&props).ok().filter(|b| !b.is_empty());
        let artist = props.Artist()?.to_string();
        let album = props.AlbumTitle()?.to_string();
        let art_key = thumb.as_ref().map(|_| format!("smtc:{title}\u{0}{artist}\u{0}{album}")).unwrap_or_default();
        let id = s.SourceAppUserModelId()?.to_string();
        let mut players = vec![];
        for other in sessions().unwrap_or_default() {
            let Ok(oid) = other.SourceAppUserModelId() else { continue };
            let playing = other.GetPlaybackInfo().and_then(|i| i.PlaybackStatus()).map(|st| st == Status::Playing).unwrap_or(false);
            players.push(Source { name: app_name(&oid.to_string()), id: oid.to_string(), playing });
        }
        let repeat = info.AutoRepeatMode().and_then(|r| r.Value()).ok().map(|r| match r {
            RepeatMode::Track => "track",
            RepeatMode::List => "playlist",
            _ => "none",
        });
        let now = Now {
            player: app_name(&id),
            player_id: id,
            shuffle: info.IsShuffleActive().and_then(|b| b.Value()).ok(),
            repeat,
            players,
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

    pub async fn control(action: Action) -> Result<(), String> {
        tauri::async_runtime::spawn_blocking(move || -> windows::core::Result<bool> {
            let s = session()?;
            match action {
                Action::PlayPause => s.TryTogglePlayPauseAsync()?.join(),
                Action::Next => s.TrySkipNextAsync()?.join(),
                Action::Previous => s.TrySkipPreviousAsync()?.join(),
                Action::Seek(ms) => s.TryChangePlaybackPositionAsync(ms as i64 * 10_000)?.join(),
                Action::Shuffle(on) => s.TryChangeShuffleActiveAsync(on)?.join(),
                Action::Repeat(mode) => s
                    .TryChangeAutoRepeatModeAsync(match mode {
                        "track" => RepeatMode::Track,
                        "playlist" => RepeatMode::List,
                        _ => RepeatMode::None,
                    })?
                    .join(),
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
    use super::{Action, Now};

    pub async fn now() -> Option<(Now, Option<Vec<u8>>)> {
        None
    }

    pub async fn control(_action: Action) -> Result<(), String> {
        Err("not supported on this system".into())
    }
}

#[cfg(target_os = "linux")]
mod loudness {
    use std::process::Stdio;
    use std::time::Duration;

    use tauri::{AppHandle, Manager, Runtime};
    use tokio::io::AsyncReadExt;

    use crate::engine::game::launch::Games;

    const RATE: usize = 11025;
    const CHUNK: usize = RATE / 20;

    fn wanted() -> bool {
        super::last().is_some_and(|t| t.playing)
    }

    pub fn start<R: Runtime>(app: &AppHandle<R>) {
        let app = app.clone();
        tauri::async_runtime::spawn(async move {
            loop {
                tokio::time::sleep(Duration::from_secs(1)).await;
                if !app.state::<Games>().any_running() || !wanted() {
                    continue;
                }
                let Ok(mut child) = tokio::process::Command::new("parec")
                    .args(["-d", "@DEFAULT_MONITOR@", "--format=s16le", "--channels=1", "--latency-msec=40", "--raw"])
                    .arg(format!("--rate={RATE}"))
                    .stdout(Stdio::piped())
                    .stderr(Stdio::null())
                    .kill_on_drop(true)
                    .spawn()
                else {
                    tokio::time::sleep(Duration::from_secs(30)).await;
                    continue;
                };
                let Some(mut out) = child.stdout.take() else { continue };
                let mut buf = vec![0u8; CHUNK * 2];
                while app.state::<Games>().any_running() && wanted() {
                    if out.read_exact(&mut buf).await.is_err() {
                        break;
                    }
                    let sum: f64 = buf.chunks_exact(2).map(|b| f64::from(i16::from_le_bytes([b[0], b[1]])).powi(2)).sum();
                    let rms = (sum / CHUNK as f64).sqrt() / 32768.0;
                    app.state::<Games>().level(rms.min(1.0) as f32);
                }
                let _ = child.kill().await;
                app.state::<Games>().level(-1.0);
            }
        });
    }
}

#[cfg(not(target_os = "linux"))]
mod loudness {
    use tauri::{AppHandle, Runtime};

    pub fn start<R: Runtime>(_app: &AppHandle<R>) {}
}
