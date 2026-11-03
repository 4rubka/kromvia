
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
            let _ = started.send(activity(&inst, None));
        }
    });
    let handle = app.clone();
    let server = tx.clone();
    app.listen_any("game-server", move |e| {
        let v: Value = serde_json::from_str(e.payload()).unwrap_or_default();
        let run = handle.state::<Games>().list().into_iter().find(|r| Some(r.run_id) == v["runId"].as_u64());
        if let Some(inst) = run.and_then(|r| instances::get(&r.instance_id).ok()) {
            let _ = server.send(activity(&inst, v["host"].as_str()));
        }
    });
    let handle = app.clone();
    app.listen_any("game-exit", move |_| {
        let next = handle.state::<Games>().list().into_iter().find_map(|r| instances::get(&r.instance_id).ok());
        let _ = tx.send(next.map_or_else(|| idle.clone(), |i| activity(&i, None)));
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

fn now() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_secs()).unwrap_or(0)
}

fn idle_activity() -> Value {
    json!({
        "details": "In the launcher",
        "timestamps": { "start": now() },
        "assets": { "large_image": "logo", "large_text": "Kromvia" },
    })
}

fn activity(inst: &Instance, host: Option<&str>) -> Value {
    let state = match &inst.loader {
        Some(l) => format!("Minecraft {} · {}", inst.mc_version, loader_name(l.kind)),
        None => format!("Minecraft {}", inst.mc_version),
    };
    let mut a = json!({
        "details": inst.name,
        "state": state,
        "timestamps": { "start": now() },
        "assets": { "large_image": "logo", "large_text": "Kromvia" },
    });
    let secret = host.map(|h| json!({ "host": h, "mc": inst.mc_version }).to_string());
    if let (Some(h), Some(secret)) = (host, secret.filter(|s| s.len() <= 128)) {
        a["party"] = json!({ "id": format!("kromvia:{h}") });
        a["secrets"] = json!({ "join": secret });
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
        png::ColorType::GrayscaleAlpha => buf.chunks_exact(2).flat_map(|p| [p[0], p[0], p[0], p[1]]).collect(),
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
