
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use serde_json::{json, Value};
use tauri::{AppHandle, Emitter, Listener, Manager, Runtime};
use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt};
use tokio::sync::mpsc::{unbounded_channel, UnboundedReceiver};

use crate::engine::game::launch::Games;
use crate::engine::game::loaders::LoaderKind;
use crate::engine::instances::{self, Instance};

const APP_ID: &str = "1565421148207058984";

trait Io: AsyncRead + AsyncWrite + Unpin + Send {}
impl<T: AsyncRead + AsyncWrite + Unpin + Send> Io for T {}

const HANDSHAKE: u32 = 0;
const FRAME: u32 = 1;
const REFRESH: Duration = Duration::from_secs(30);

pub fn start<R: Runtime>(app: &AppHandle<R>) {
    if APP_ID.is_empty() {
        return;
    }
    let (tx, rx) = unbounded_channel::<Value>();
    let idle = idle_activity();
    tauri::async_runtime::spawn(run(app.clone(), rx, idle.clone()));
    let started = tx.clone();
    app.listen_any("game-started", move |e| {
        let run: Value = serde_json::from_str(e.payload()).unwrap_or_default();
        if let Some(inst) = run["instanceId"].as_str().and_then(|id| instances::get(id).ok()) {
            let _ = started.send(activity(&inst, &Scene::Menu));
        }
    });
    let handle = app.clone();
    let server = tx.clone();
    app.listen_any("game-server", move |e| {
        let v: Value = serde_json::from_str(e.payload()).unwrap_or_default();
        let run = handle.state::<Games>().list().into_iter().find(|r| Some(r.run_id) == v["runId"].as_u64());
        let Some(inst) = run.and_then(|r| instances::get(&r.instance_id).ok()) else { return };
        let Some(host) = v["host"].as_str().map(String::from) else {
            let _ = server.send(activity(&inst, &Scene::Menu));
            return;
        };
        let _ = server.send(activity(&inst, &Scene::Server(host.clone(), None)));
        let server = server.clone();
        tauri::async_runtime::spawn(async move {
            if let Ok(st) = crate::engine::servers::status(&host).await {
                let _ = server.send(activity(&inst, &Scene::Server(host, Some((st.online, st.max)))));
            }
        });
    });
    let handle = app.clone();
    let scene = tx.clone();
    app.listen_any("game-scene", move |e| {
        let v: Value = serde_json::from_str(e.payload()).unwrap_or_default();
        let run = handle.state::<Games>().list().into_iter().find(|r| Some(r.run_id) == v["runId"].as_u64());
        let Some(inst) = run.and_then(|r| instances::get(&r.instance_id).ok()) else { return };
        let next = match v["scene"].as_str() {
            Some("singleplayer") => Scene::Singleplayer,
            Some("room") => Scene::Room,
            _ => Scene::Menu,
        };
        let _ = scene.send(activity(&inst, &next));
    });
    let handle = app.clone();
    app.listen_any("game-exit", move |_| {
        let next = handle.state::<Games>().list().into_iter().find_map(|r| instances::get(&r.instance_id).ok());
        let _ = tx.send(next.map_or_else(|| idle.clone(), |i| activity(&i, &Scene::Menu)));
    });
}

fn loader_name(kind: LoaderKind) -> &'static str {
    match kind {
        LoaderKind::Fabric => "Fabric",
        LoaderKind::Quilt => "Quilt",
        LoaderKind::LegacyFabric => "Legacy Fabric",
        LoaderKind::Ornithe => "Ornithe",
        LoaderKind::Forge => "Forge",
        LoaderKind::NeoForge => "NeoForge",
    }
}

const SITE: &str = "https://kromvia.com";

fn now() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_secs()).unwrap_or(0)
}

fn idle_activity() -> Value {
    json!({
        "details": "In the launcher",
        "details_url": SITE,
        "timestamps": { "start": now() },
        "assets": { "large_image": "logo", "large_text": "Kromvia", "large_url": SITE },
        "buttons": [{ "label": "Kromvia", "url": SITE }],
    })
}

pub enum Scene {
    Menu,
    Singleplayer,
    Room,
    Server(String, Option<(u32, u32)>),
}

fn activity(inst: &Instance, scene: &Scene) -> Value {
    let version = match &inst.loader {
        Some(l) => format!("Minecraft {} · {}", inst.mc_version, loader_name(l.kind)),
        None => format!("Minecraft {}", inst.mc_version),
    };
    let (details, state) = match scene {
        Scene::Menu => (format!("In the menu · {}", inst.name), version.clone()),
        Scene::Singleplayer => ("Playing singleplayer".to_string(), format!("{} · {version}", inst.name)),
        Scene::Room => ("Hosting a room for friends".to_string(), format!("{} · {version}", inst.name)),
        Scene::Server(host, _) => (format!("Playing on {host}"), format!("{} · {version}", inst.name)),
    };
    let mut a = json!({
        "details": details,
        "details_url": SITE,
        "state": state,
        "timestamps": { "start": now() },
        "assets": { "large_image": "logo", "large_text": format!("Kromvia · {version}"), "large_url": SITE },
    });
    let Scene::Server(host, players) = scene else {
        a["buttons"] = json!([{ "label": "Kromvia", "url": SITE }]);
        return a;
    };
    a["assets"]["small_image"] = json!(format!("https://api.mcstatus.io/v2/icon/{host}"));
    a["assets"]["small_text"] = json!(match players {
        Some((online, max)) => format!("{host} · {online}/{max} online"),
        None => host.clone(),
    });
    a["party"] = json!({ "id": format!("kromvia:{host}") });
    if let Some(&(online, max)) = players.as_ref().filter(|(o, m)| *m > 0 && o <= m) {
        a["party"]["size"] = json!([online.max(1), max]);
    }
    let secret = json!({ "host": host, "mc": inst.mc_version }).to_string();
    if secret.len() <= 128 {
        a["secrets"] = json!({ "join": secret });
    } else {
        a["buttons"] = json!([{ "label": "Kromvia", "url": SITE }]);
    }
    a
}

fn joined<R: Runtime>(app: &AppHandle<R>, msg: &Value) {
    if msg["evt"] != "ACTIVITY_JOIN" {
        return;
    }
    let secret: Value = msg["data"]["secret"].as_str().and_then(|s| serde_json::from_str(s).ok()).unwrap_or_default();
    let Some(host) = secret["host"].as_str().filter(|h| crate::engine::game::launch::valid_address(h)) else {
        return;
    };
    let _ = app.emit("discord-join", json!({ "host": host, "mc": secret["mc"] }));
    if let Some(w) = app.get_webview_window("main") {
        let _ = w.show();
        let _ = w.set_focus();
    }
}

async fn run<R: Runtime>(app: AppHandle<R>, mut rx: UnboundedReceiver<Value>, mut current: Value) {
    loop {
        let Some(c) = connect().await else {
            match tokio::time::timeout(REFRESH, rx.recv()).await {
                Ok(Some(next)) => current = next,
                Ok(None) => return,
                Err(_) => {}
            }
            continue;
        };
        let (mut rd, mut wr) = tokio::io::split(c);
        let subscribe = json!({ "cmd": "SUBSCRIBE", "evt": "ACTIVITY_JOIN", "nonce": nonce() });
        if send(&mut wr, FRAME, &subscribe).await.is_err() || set_activity(&mut wr, &current).await.is_err() {
            continue;
        }
        let (etx, mut erx) = unbounded_channel::<Value>();
        let reader = tauri::async_runtime::spawn(async move {
            while let Ok(msg) = recv(&mut rd).await {
                if etx.send(msg).is_err() {
                    break;
                }
            }
        });
        loop {
            tokio::select! {
                next = rx.recv() => {
                    let Some(next) = next else {
                        reader.abort();
                        return;
                    };
                    current = next;
                    while let Ok(newer) = rx.try_recv() {
                        current = newer;
                    }
                    if set_activity(&mut wr, &current).await.is_err() {
                        break;
                    }
                }
                msg = erx.recv() => match msg {
                    Some(msg) => joined(&app, &msg),
                    None => break,
                },
            }
        }
        reader.abort();
    }
}

fn nonce() -> String {
    format!("{:x}", SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_nanos()).unwrap_or(0))
}

async fn set_activity(c: &mut (impl AsyncWrite + Unpin), activity: &Value) -> Result<(), String> {
    let msg = json!({ "cmd": "SET_ACTIVITY", "args": { "pid": std::process::id(), "activity": activity }, "nonce": nonce() });
    send(c, FRAME, &msg).await
}

async fn send(c: &mut (impl AsyncWrite + Unpin), op: u32, msg: &Value) -> Result<(), String> {
    let body = msg.to_string().into_bytes();
    let mut frame = Vec::with_capacity(8 + body.len());
    frame.extend_from_slice(&op.to_le_bytes());
    frame.extend_from_slice(&(body.len() as u32).to_le_bytes());
    frame.extend_from_slice(&body);
    c.write_all(&frame).await.map_err(|e| e.to_string())?;
    c.flush().await.map_err(|e| e.to_string())
}

async fn recv(c: &mut (impl AsyncRead + Unpin)) -> Result<Value, String> {
    let mut head = [0u8; 8];
    c.read_exact(&mut head).await.map_err(|e| e.to_string())?;
    let len = u32::from_le_bytes(head[4..].try_into().unwrap()) as usize;
    if len > 64 * 1024 {
        return Err("frame too large".to_string());
    }
    let mut body = vec![0u8; len];
    c.read_exact(&mut body).await.map_err(|e| e.to_string())?;
    serde_json::from_slice(&body).map_err(|e| e.to_string())
}

async fn connect() -> Option<Box<dyn Io>> {
    let mut c = socket().await?;
    send(&mut c, HANDSHAKE, &json!({ "v": 1, "client_id": APP_ID })).await.ok()?;
    tokio::time::timeout(Duration::from_secs(5), recv(&mut c)).await.ok()?.ok().filter(|v| v["evt"] == "READY")?;
    Some(c)
}

#[cfg(unix)]
async fn socket() -> Option<Box<dyn Io>> {
    let mut dirs: Vec<std::path::PathBuf> =
        ["XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"].iter().filter_map(std::env::var_os).map(Into::into).collect();
    dirs.push("/tmp".into());
    for dir in dirs {
        for sub in ["", "app/com.discordapp.Discord", "snap.discord", ".flatpak/dev.vencord.Vesktop/xdg-run"] {
            for i in 0..10 {
                if let Ok(s) = tokio::net::UnixStream::connect(dir.join(sub).join(format!("discord-ipc-{i}"))).await {
                    return Some(Box::new(s));
                }
            }
        }
    }
    None
}

#[cfg(windows)]
async fn socket() -> Option<Box<dyn Io>> {
    (0..10).find_map(|i| {
        let p = tokio::net::windows::named_pipe::ClientOptions::new().open(format!(r"\\.\pipe\discord-ipc-{i}")).ok()?;
        Some(Box::new(p) as Box<dyn Io>)
    })
}

static CLIPBOARD: std::sync::Mutex<Option<arboard::Clipboard>> = std::sync::Mutex::new(None);

fn rgba(path: &std::path::Path) -> Result<(usize, usize, Vec<u8>), String> {
    let file = std::io::BufReader::new(std::fs::File::open(path).map_err(|e| e.to_string())?);
    let mut decoder = png::Decoder::new(file);
    decoder.set_transformations(png::Transformations::normalize_to_color8() | png::Transformations::ALPHA);
    let mut reader = decoder.read_info().map_err(|e| e.to_string())?;
    let mut buf = vec![0; reader.output_buffer_size().ok_or("image too large")?];
    let info = reader.next_frame(&mut buf).map_err(|e| e.to_string())?;
    buf.truncate(info.buffer_size());
    let pixels = match info.color_type {
        png::ColorType::Rgba => buf,
        png::ColorType::GrayscaleAlpha => buf.as_chunks::<2>().0.iter().flat_map(|&[g, a]| [g, g, g, a]).collect(),
        other => return Err(format!("unexpected colour type {other:?}")),
    };
    Ok((info.width as usize, info.height as usize, pixels))
}

pub fn screenshot_to_discord(path: &std::path::Path) -> Result<(), String> {
    let (width, height, bytes) = rgba(path)?;
    let mut slot = CLIPBOARD.lock().unwrap_or_else(|e| e.into_inner());
    if slot.is_none() {
        *slot = Some(arboard::Clipboard::new().map_err(|e| e.to_string())?);
    }
    let image = arboard::ImageData { width, height, bytes: bytes.into() };
    slot.as_mut().unwrap().set_image(image).map_err(|e| e.to_string())?;
    drop(slot);
    tauri_plugin_opener::open_url("discord://-/channels/@me", None::<&str>)
        .or_else(|_| tauri_plugin_opener::open_url("https://discord.com/channels/@me", None::<&str>))
        .map_err(|e| e.to_string())
}
