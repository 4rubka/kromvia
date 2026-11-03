
use std::time::{Duration, Instant};

use serde::{Deserialize, Serialize};
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpStream;
use tokio::time::timeout;

use crate::engine::instances;
use crate::engine::nbt::{self, Tag};

#[derive(Serialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct ServerStatus {
    pub online: u32,
    pub max: u32,
    pub version: String,
    pub latency_ms: u32,
}

#[derive(Deserialize)]
struct Response {
    players: Option<Players>,
    version: Option<Version>,
}

#[derive(Deserialize)]
struct Players {
    online: u32,
    max: u32,
}

#[derive(Deserialize)]
struct Version {
    name: String,
}

const TIMEOUT: Duration = Duration::from_secs(4);
const MAX_PACKET: usize = 1 << 20;

fn write_varint(buf: &mut Vec<u8>, mut v: i32) {
    loop {
        let mut b = (v & 0x7f) as u8;
        v = ((v as u32) >> 7) as i32;
        if v != 0 {
            b |= 0x80;
        }
        buf.push(b);
        if v == 0 {
            break;
        }
    }
}

async fn read_varint(s: &mut TcpStream) -> Result<i32, String> {
    let mut value = 0i32;
    for i in 0..5 {
        let b = s.read_u8().await.map_err(|e| e.to_string())?;
        value |= ((b & 0x7f) as i32) << (7 * i);
        if b & 0x80 == 0 {
            return Ok(value);
        }
    }
    Err("bad varint".into())
}

fn packet(id: i32, body: &[u8]) -> Vec<u8> {
    let mut inner = Vec::new();
    write_varint(&mut inner, id);
    inner.extend_from_slice(body);
    let mut out = Vec::new();
    write_varint(&mut out, inner.len() as i32);
    out.extend(inner);
    out
}

pub fn split_address(addr: &str) -> (String, u16) {
    match addr.rsplit_once(':') {
        Some((h, p)) if p.parse::<u16>().is_ok() => (h.to_string(), p.parse().unwrap()),
        _ => (addr.to_string(), 25565),
    }
}

pub async fn status(addr: &str) -> Result<ServerStatus, String> {
    let (host, port) = split_address(addr);
    timeout(TIMEOUT, ping(&host, port)).await.map_err(|_| "timed out".to_string())?
}

async fn ping(host: &str, port: u16) -> Result<ServerStatus, String> {
    let started = Instant::now();
    let mut s = TcpStream::connect((host, port)).await.map_err(|e| e.to_string())?;
    let latency_ms = started.elapsed().as_millis() as u32;

    let mut hs = Vec::new();
    write_varint(&mut hs, -1); // protocol version: "any", for status
    write_varint(&mut hs, host.len() as i32);
    hs.extend_from_slice(host.as_bytes());
    hs.extend_from_slice(&port.to_be_bytes());
    write_varint(&mut hs, 1); // next state: status
    s.write_all(&packet(0x00, &hs)).await.map_err(|e| e.to_string())?;
    s.write_all(&packet(0x00, &[])).await.map_err(|e| e.to_string())?;

    let len = read_varint(&mut s).await? as usize;
    if len == 0 || len > MAX_PACKET {
        return Err("bad status packet".into());
    }
    let id = read_varint(&mut s).await?;
    if id != 0x00 {
        return Err("unexpected packet".into());
    }
    let json_len = read_varint(&mut s).await? as usize;
    if json_len > MAX_PACKET {
        return Err("status too large".into());
    }
    let mut json = vec![0u8; json_len];
    s.read_exact(&mut json).await.map_err(|e| e.to_string())?;
    let r: Response = serde_json::from_slice(&json).map_err(|e| e.to_string())?;
    let players = r.players.unwrap_or(Players { online: 0, max: 0 });
    Ok(ServerStatus {
        online: players.online,
        max: players.max,
        version: r.version.map(|v| v.name).unwrap_or_default(),
        latency_ms,
    })
}

#[derive(Serialize, Deserialize, Clone, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct SavedServer {
    pub name: String,
    pub address: String,
    #[serde(default)]
    pub icon: Option<String>,
    #[serde(default)]
    pub pinned: bool,
}

fn servers_dat(instance_id: &str) -> Result<std::path::PathBuf, String> {
    Ok(instances::game_dir(instance_id)?.join("servers.dat"))
}

fn read_root(instance_id: &str) -> Result<Tag, String> {
    match std::fs::read(servers_dat(instance_id)?) {
        Ok(bytes) => nbt::parse(&bytes),
        Err(e) if e.kind() == std::io::ErrorKind::NotFound => Ok(Tag::Compound(vec![])),
        Err(e) => Err(e.to_string()),
    }
}

fn entries(root: &Tag) -> &[Tag] {
    match root.get("servers") {
        Some(Tag::List(_, items)) => items,
        _ => &[],
    }
}

fn pins_path(instance_id: &str) -> Result<std::path::PathBuf, String> {
    Ok(instances::dir(instance_id)?.join("pinned-servers.json"))
}

fn pins(instance_id: &str) -> Result<Vec<String>, String> {
    match std::fs::read(pins_path(instance_id)?) {
        Ok(bytes) => Ok(serde_json::from_slice(&bytes).unwrap_or_default()),
        Err(e) if e.kind() == std::io::ErrorKind::NotFound => Ok(vec![]),
        Err(e) => Err(e.to_string()),
    }
}

pub fn saved(instance_id: &str) -> Result<Vec<SavedServer>, String> {
    let root = read_root(instance_id)?;
    let pins = pins(instance_id)?;
    let mut list: Vec<SavedServer> = entries(&root)
        .iter()
        .filter_map(|e| {
            let s = |k: &str| e.get(k).and_then(Tag::str).map(String::from);
            let address = s("ip")?;
            Some(SavedServer {
                name: s("name").unwrap_or_default(),
                pinned: pins.contains(&address),
                address,
                icon: s("icon").map(|i| format!("data:image/png;base64,{i}")),
            })
        })
        .collect();
    list.sort_by_key(|s| !s.pinned);
    Ok(list)
}

pub fn set_pinned(instance_id: &str, address: &str, pinned: bool) -> Result<(), String> {
    let mut pins = pins(instance_id)?;
    pins.retain(|a| a != address);
    if pinned {
        pins.push(address.to_string());
    }
    std::fs::write(pins_path(instance_id)?, serde_json::to_vec(&pins).map_err(|e| e.to_string())?).map_err(|e| e.to_string())?;
    apply_pins(instance_id)
}

pub fn apply_pins(instance_id: &str) -> Result<(), String> {
    let pins = pins(instance_id)?;
    if pins.is_empty() {
        return Ok(());
    }
    let mut root = read_root(instance_id)?;
    let mut list = entries(&root).to_vec();
    let is_pinned = |e: &Tag| e.get("ip").and_then(Tag::str).is_some_and(|a| pins.iter().any(|p| p == a));
    if list.iter().skip_while(|e| is_pinned(e)).all(|e| !is_pinned(e)) {
        return Ok(());
    }
    list.sort_by_key(|e| !is_pinned(e));
    store(instance_id, &mut root, list)
}

fn store(instance_id: &str, root: &mut Tag, list: Vec<Tag>) -> Result<(), String> {
    root.set("servers", Tag::List(10, list));
    let path = servers_dat(instance_id)?;
    let tmp = path.with_extension("dat.tmp");
    std::fs::write(&tmp, nbt::write(root)).map_err(|e| e.to_string())?;
    std::fs::rename(&tmp, &path).map_err(|e| e.to_string())
}

pub fn add_saved(instance_id: &str, name: &str, address: &str) -> Result<(), String> {
    let name = name.trim();
    if name.is_empty() || name.chars().count() > 64 {
        return Err("name must be 1–64 characters".into());
    }
    let mut root = read_root(instance_id)?;
    let mut list = entries(&root).to_vec();
    match list.iter_mut().find(|e| e.get("ip").and_then(Tag::str) == Some(address)) {
        Some(e) => e.set("name", Tag::String(name.into())),
        None => list.insert(0, Tag::Compound(vec![("name".into(), Tag::String(name.into())), ("ip".into(), Tag::String(address.into()))])),
    }
    store(instance_id, &mut root, list)
}

pub fn remove_saved(instance_id: &str, address: &str) -> Result<(), String> {
    let mut root = read_root(instance_id)?;
    let list = entries(&root).iter().filter(|e| e.get("ip").and_then(Tag::str) != Some(address)).cloned().collect();
    store(instance_id, &mut root, list)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn varints_match_the_spec() {
        let enc = |v| {
            let mut b = Vec::new();
            write_varint(&mut b, v);
            b
        };
        assert_eq!(enc(0), [0x00]);
        assert_eq!(enc(127), [0x7f]);
        assert_eq!(enc(128), [0x80, 0x01]);
        assert_eq!(enc(25565), [0xdd, 0xc7, 0x01]);
        assert_eq!(enc(-1), [0xff, 0xff, 0xff, 0xff, 0x0f]);
    }

    #[test]
    fn splits_addresses() {
        assert_eq!(split_address("mc.hypixel.net"), ("mc.hypixel.net".into(), 25565));
        assert_eq!(split_address("localhost:25570"), ("localhost".into(), 25570));
    }

    #[test]
    fn edits_servers_dat() {
        crate::engine::core::paths::init(std::env::temp_dir().join(format!("kromvia-servers-{}", std::process::id())));
        let inst = instances::create("Servers", "1.21.4", None).unwrap();
        assert_eq!(saved(&inst.id).unwrap(), []);
        add_saved(&inst.id, "Hypixel", "mc.hypixel.net").unwrap();
        add_saved(&inst.id, "Local", "localhost:25570").unwrap();
        add_saved(&inst.id, "Hypixel!", "mc.hypixel.net").unwrap();
        let list = saved(&inst.id).unwrap();
        assert_eq!(list.iter().map(|s| (s.name.as_str(), s.address.as_str())).collect::<Vec<_>>(), [("Local", "localhost:25570"), ("Hypixel!", "mc.hypixel.net")]);
        add_saved(&inst.id, "Third", "third.example").unwrap();
        set_pinned(&inst.id, "mc.hypixel.net", true).unwrap();
        let order = |id: &str| saved(id).unwrap().into_iter().map(|s| (s.address, s.pinned)).collect::<Vec<_>>();
        assert_eq!(order(&inst.id), [("mc.hypixel.net".into(), true), ("third.example".into(), false), ("localhost:25570".into(), false)]);
        add_saved(&inst.id, "Top", "top.example").unwrap();
        apply_pins(&inst.id).unwrap();
        assert_eq!(entries(&read_root(&inst.id).unwrap())[0].get("ip").and_then(Tag::str), Some("mc.hypixel.net"));
        set_pinned(&inst.id, "mc.hypixel.net", false).unwrap();
        assert!(saved(&inst.id).unwrap().iter().all(|s| !s.pinned));
        remove_saved(&inst.id, "localhost:25570").unwrap();
        assert_eq!(saved(&inst.id).unwrap().len(), 3);
        instances::delete(&inst.id).unwrap();
    }

    #[tokio::test]
    #[ignore]
    async fn pings_hypixel() {
        for a in ["mc.hypixel.net", "play.cubecraft.net", "play.wynncraft.com"] {
            println!("{a}: {:?}", status(a).await);
        }
        assert!(status("mc.hypixel.net").await.unwrap().max > 0);
    }
}
