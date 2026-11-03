
use std::io::{Read, Write};

use flate2::read::GzDecoder;
use flate2::write::GzEncoder;
use flate2::Compression;

#[derive(Clone, Debug, PartialEq)]
pub enum Tag {
    Byte(i8),
    Short(i16),
    Int(i32),
    Long(i64),
    Float(f32),
    Double(f64),
    ByteArray(Vec<i8>),
    String(String),
    List(u8, Vec<Tag>),
    Compound(Vec<(String, Tag)>),
    IntArray(Vec<i32>),
    LongArray(Vec<i64>),
}

const MAX_DEPTH: u32 = 512;

impl Tag {
    fn id(&self) -> u8 {
        match self {
            Tag::Byte(_) => 1,
            Tag::Short(_) => 2,
            Tag::Int(_) => 3,
            Tag::Long(_) => 4,
            Tag::Float(_) => 5,
            Tag::Double(_) => 6,
            Tag::ByteArray(_) => 7,
            Tag::String(_) => 8,
            Tag::List(..) => 9,
            Tag::Compound(_) => 10,
            Tag::IntArray(_) => 11,
            Tag::LongArray(_) => 12,
        }
    }

    pub fn get(&self, key: &str) -> Option<&Tag> {
        match self {
            Tag::Compound(entries) => entries.iter().find(|(k, _)| k == key).map(|(_, v)| v),
            _ => None,
        }
    }

    pub fn get_mut(&mut self, key: &str) -> Option<&mut Tag> {
        match self {
            Tag::Compound(entries) => entries.iter_mut().find(|(k, _)| k == key).map(|(_, v)| v),
            _ => None,
        }
    }

    pub fn set(&mut self, key: &str, value: Tag) {
        if let Tag::Compound(entries) = self {
            match entries.iter_mut().find(|(k, _)| k == key) {
                Some((_, v)) => *v = value,
                None => entries.push((key.to_string(), value)),
            }
        }
    }

    pub fn str(&self) -> Option<&str> {
        match self {
            Tag::String(s) => Some(s),
            _ => None,
        }
    }

    pub fn int(&self) -> Option<i64> {
        match *self {
            Tag::Byte(v) => Some(v as i64),
            Tag::Short(v) => Some(v as i64),
            Tag::Int(v) => Some(v as i64),
            Tag::Long(v) => Some(v),
            _ => None,
        }
    }
}

struct Reader<'a> {
    data: &'a [u8],
    pos: usize,
}

impl Reader<'_> {
    fn take(&mut self, n: usize) -> Result<&[u8], String> {
        let end = self.pos.checked_add(n).filter(|&e| e <= self.data.len()).ok_or("truncated NBT")?;
        let out = &self.data[self.pos..end];
        self.pos = end;
        Ok(out)
    }

    fn arr<const N: usize>(&mut self) -> Result<[u8; N], String> {
        Ok(self.take(N)?.try_into().unwrap())
    }

    fn len(&mut self, item: usize) -> Result<usize, String> {
        let n = i32::from_be_bytes(self.arr()?);
        let n = usize::try_from(n).map_err(|_| "negative NBT length")?;
        if n.saturating_mul(item.max(1)) > self.data.len() - self.pos {
            return Err("NBT length past end of file".into());
        }
        Ok(n)
    }

    fn string(&mut self) -> Result<String, String> {
        let n = u16::from_be_bytes(self.arr()?) as usize;
        Ok(mutf8_decode(self.take(n)?))
    }

    fn payload(&mut self, id: u8, depth: u32) -> Result<Tag, String> {
        if depth > MAX_DEPTH {
            return Err("NBT nested too deep".into());
        }
        Ok(match id {
            1 => Tag::Byte(self.arr::<1>()?[0] as i8),
            2 => Tag::Short(i16::from_be_bytes(self.arr()?)),
            3 => Tag::Int(i32::from_be_bytes(self.arr()?)),
            4 => Tag::Long(i64::from_be_bytes(self.arr()?)),
            5 => Tag::Float(f32::from_be_bytes(self.arr()?)),
            6 => Tag::Double(f64::from_be_bytes(self.arr()?)),
            7 => {
                let n = self.len(1)?;
                Tag::ByteArray(self.take(n)?.iter().map(|&b| b as i8).collect())
            }
            8 => Tag::String(self.string()?),
            9 => {
                let kind = self.arr::<1>()?[0];
                let n = self.len(1)?;
                let items = (0..n).map(|_| self.payload(kind, depth + 1)).collect::<Result<_, _>>()?;
                Tag::List(kind, items)
            }
            10 => {
                let mut entries = Vec::new();
                loop {
                    let kind = self.arr::<1>()?[0];
                    if kind == 0 {
                        break;
                    }
                    let name = self.string()?;
                    entries.push((name, self.payload(kind, depth + 1)?));
                }
                Tag::Compound(entries)
            }
            11 => {
                let n = self.len(4)?;
                Tag::IntArray((0..n).map(|_| self.arr().map(i32::from_be_bytes)).collect::<Result<_, _>>()?)
            }
            12 => {
                let n = self.len(8)?;
                Tag::LongArray((0..n).map(|_| self.arr().map(i64::from_be_bytes)).collect::<Result<_, _>>()?)
            }
            _ => return Err(format!("unknown NBT tag {id}")),
        })
    }
}

fn write_string(out: &mut Vec<u8>, s: &str) {
    let bytes = mutf8_encode(s);
    let n = bytes.len().min(u16::MAX as usize);
    out.extend_from_slice(&(n as u16).to_be_bytes());
    out.extend_from_slice(&bytes[..n]);
}

fn write_payload(out: &mut Vec<u8>, tag: &Tag) {
    let len = |out: &mut Vec<u8>, n: usize| out.extend_from_slice(&(n as i32).to_be_bytes());
    match tag {
        Tag::Byte(v) => out.push(*v as u8),
        Tag::Short(v) => out.extend_from_slice(&v.to_be_bytes()),
        Tag::Int(v) => out.extend_from_slice(&v.to_be_bytes()),
        Tag::Long(v) => out.extend_from_slice(&v.to_be_bytes()),
        Tag::Float(v) => out.extend_from_slice(&v.to_be_bytes()),
        Tag::Double(v) => out.extend_from_slice(&v.to_be_bytes()),
        Tag::ByteArray(v) => {
            len(out, v.len());
            out.extend(v.iter().map(|&b| b as u8));
        }
        Tag::String(s) => write_string(out, s),
        Tag::List(kind, items) => {
            out.push(items.first().map_or(*kind, Tag::id));
            len(out, items.len());
            items.iter().for_each(|t| write_payload(out, t));
        }
        Tag::Compound(entries) => {
            for (name, v) in entries {
                out.push(v.id());
                write_string(out, name);
                write_payload(out, v);
            }
            out.push(0);
        }
        Tag::IntArray(v) => {
            len(out, v.len());
            v.iter().for_each(|x| out.extend_from_slice(&x.to_be_bytes()));
        }
        Tag::LongArray(v) => {
            len(out, v.len());
            v.iter().for_each(|x| out.extend_from_slice(&x.to_be_bytes()));
        }
    }
}

pub fn parse(data: &[u8]) -> Result<Tag, String> {
    let mut r = Reader { data, pos: 0 };
    if r.arr::<1>()?[0] != 10 {
        return Err("NBT root is not a compound".into());
    }
    r.string()?;
    r.payload(10, 0)
}

pub fn write(root: &Tag) -> Vec<u8> {
    let mut out = vec![10];
    write_string(&mut out, "");
    write_payload(&mut out, root);
    out
}

const MAX_FILE: u64 = 64 << 20;

pub fn parse_gzip(data: &[u8]) -> Result<Tag, String> {
    let mut raw = Vec::new();
    GzDecoder::new(data).take(MAX_FILE).read_to_end(&mut raw).map_err(|e| e.to_string())?;
    parse(&raw)
}

pub fn write_gzip(root: &Tag) -> Vec<u8> {
    let mut enc = GzEncoder::new(Vec::new(), Compression::default());
    enc.write_all(&write(root)).unwrap();
    enc.finish().unwrap()
}

fn mutf8_decode(bytes: &[u8]) -> String {
    let mut units = Vec::with_capacity(bytes.len());
    let mut i = 0;
    while i < bytes.len() {
        let b = bytes[i] as u16;
        let more = |k: usize| bytes.get(i + k).map_or(0, |&c| (c & 0x3f) as u16);
        let (unit, n) = match b {
            0x00..=0x7f => (b, 1),
            0xc0..=0xdf => (((b & 0x1f) << 6) | more(1), 2),
            0xe0..=0xef => (((b & 0x0f) << 12) | (more(1) << 6) | more(2), 3),
            _ => (0xfffd, 1),
        };
        units.push(unit);
        i += n;
    }
    String::from_utf16_lossy(&units)
}

fn mutf8_encode(s: &str) -> Vec<u8> {
    let mut out = Vec::with_capacity(s.len());
    for u in s.encode_utf16() {
        match u {
            0x01..=0x7f => out.push(u as u8),
            0x00 | 0x80..=0x7ff => out.extend([0xc0 | (u >> 6) as u8, 0x80 | (u & 0x3f) as u8]),
            _ => out.extend([0xe0 | (u >> 12) as u8, 0x80 | ((u >> 6) & 0x3f) as u8, 0x80 | (u & 0x3f) as u8]),
        }
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    fn sample() -> Tag {
        Tag::Compound(vec![(
            "Data".into(),
            Tag::Compound(vec![
                ("LevelName".into(), Tag::String("Світ 🌍\0x".into())),
                ("LastPlayed".into(), Tag::Long(1_700_000_000_000)),
                ("hardcore".into(), Tag::Byte(1)),
                ("Spawn".into(), Tag::IntArray(vec![1, -2, 3])),
                ("Empty".into(), Tag::List(10, vec![])),
                ("Pos".into(), Tag::List(6, vec![Tag::Double(0.5), Tag::Double(-1.0)])),
                ("Bytes".into(), Tag::ByteArray(vec![-1, 0, 1])),
                ("Longs".into(), Tag::LongArray(vec![i64::MIN])),
                ("F".into(), Tag::Float(1.5)),
                ("S".into(), Tag::Short(-7)),
                ("I".into(), Tag::Int(42)),
            ]),
        )])
    }

    #[test]
    fn round_trips() {
        let t = sample();
        assert_eq!(parse(&write(&t)).unwrap(), t);
        assert_eq!(parse_gzip(&write_gzip(&t)).unwrap(), t);
        let data = t.get("Data").unwrap();
        assert_eq!(data.get("LevelName").unwrap().str(), Some("Світ 🌍\0x"));
        assert_eq!(data.get("hardcore").unwrap().int(), Some(1));
    }

    #[test]
    fn encodes_like_java() {
        assert_eq!(mutf8_encode("\0"), [0xc0, 0x80]);
        assert_eq!(mutf8_encode("😀"), [0xed, 0xa0, 0xbd, 0xed, 0xb8, 0x80]);
        assert_eq!(mutf8_decode(&[0xed, 0xa0, 0xbd, 0xed, 0xb8, 0x80]), "😀");
    }

    #[test]
    fn rejects_garbage() {
        assert!(parse(&[]).is_err());
        assert!(parse(&[8, 0, 0]).is_err());
        assert!(parse(&[10, 0, 0, 7, 0, 1, b'a', 0x7f, 0xff, 0xff, 0xff]).is_err());
        let mut deep = vec![10, 0, 0];
        for _ in 0..600 {
            deep.extend([10, 0, 1, b'a']);
        }
        assert!(parse(&deep).is_err());
    }

    #[test]
    #[ignore]
    fn round_trips_real_files() {
        let dir = std::path::PathBuf::from(std::env::var("KROMVIA_NBT_DIR").unwrap());
        let mut n = 0;
        for (rel, path) in crate::engine::content::mrpack::walk(&dir, "") {
            let gzip = rel.ends_with("/level.dat");
            if !gzip && !rel.ends_with("/servers.dat") {
                continue;
            }
            let bytes = std::fs::read(&path).unwrap();
            let raw = if gzip {
                let mut raw = Vec::new();
                GzDecoder::new(&bytes[..]).read_to_end(&mut raw).unwrap();
                raw
            } else {
                bytes
            };
            let tag = parse(&raw).unwrap_or_else(|e| panic!("{rel}: {e}"));
            assert!(write(&tag) == raw, "{rel} does not round-trip");
            if gzip {
                println!("{rel}: {:?}", tag.get("Data").and_then(|d| d.get("LevelName")));
            }
            n += 1;
        }
        println!("{n} files");
        assert!(n > 0);
    }

    #[test]
    fn sets_keys() {
        let mut t = Tag::Compound(vec![]);
        t.set("a", Tag::Int(1));
        t.set("a", Tag::Int(2));
        t.set("b", Tag::Int(3));
        assert_eq!(t, Tag::Compound(vec![("a".into(), Tag::Int(2)), ("b".into(), Tag::Int(3))]));
        if let Some(Tag::Int(v)) = t.get_mut("b") {
            *v = 4;
        }
        assert_eq!(t.get("b").unwrap().int(), Some(4));
    }
}
