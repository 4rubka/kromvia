
use std::collections::HashMap;
use std::io::{Cursor, Read, Seek};

use serde::Serialize;

use super::{list, DISABLED};
use crate::engine::game::loaders::LoaderKind;
use crate::engine::instances;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
enum Family {
    Fabric,
    Quilt,
    Forge,
}

#[derive(Clone, Debug, PartialEq)]
enum Range {
    Fabric(Vec<String>),
    Maven(String),
}

#[derive(Clone, Debug, Default)]
struct Meta {
    id: String,
    name: String,
    version: String,
    provides: Vec<String>,
    families: Vec<Family>,
    depends: Vec<(String, Range)>,
    breaks: Vec<(String, Range, bool)>,
    nested: Vec<Meta>,
}

#[derive(Serialize, Clone, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct Conflict {
    pub severity: &'static str,
    pub kind: &'static str,
    pub id: String,
    pub name: Option<String>,
    pub files: Vec<String>,
    pub wanted: Option<String>,
    pub found: Option<String>,
}

const NESTED_MAX: u64 = 32 * 1024 * 1024;

const BUILTIN: [&str; 11] =
    ["minecraft", "java", "fabricloader", "fabric-loader", "quilt_loader", "mixinextras", "forge", "neoforge", "javafml", "lowcodefml", "mclanguage"];

fn read_text<R: Read + Seek>(zip: &mut zip::ZipArchive<R>, name: &str) -> Option<String> {
    let mut s = String::new();
    zip.by_name(name).ok()?.take(1 << 20).read_to_string(&mut s).ok()?;
    Some(s)
}

fn fabric_range(v: &serde_json::Value) -> Range {
    match v {
        serde_json::Value::Array(a) => Range::Fabric(a.iter().filter_map(|s| s.as_str().map(String::from)).collect()),
        serde_json::Value::String(s) => Range::Fabric(vec![s.clone()]),
        _ => Range::Fabric(vec!["*".into()]),
    }
}

fn fabric_map(v: &serde_json::Value) -> Vec<(String, Range)> {
    v.as_object().map(|m| m.iter().map(|(k, v)| (k.clone(), fabric_range(v))).collect()).unwrap_or_default()
}

fn parse_fabric(text: &str, meta: &mut Meta) -> Vec<String> {
    let Ok(v) = serde_json::from_str::<serde_json::Value>(text) else { return vec![] };
    meta.id = v["id"].as_str().unwrap_or_default().to_string();
    meta.name = v["name"].as_str().unwrap_or(&meta.id).to_string();
    meta.version = v["version"].as_str().unwrap_or_default().to_string();
    meta.provides = v["provides"].as_array().map(|a| a.iter().filter_map(|s| s.as_str().map(String::from)).collect()).unwrap_or_default();
    meta.depends = fabric_map(&v["depends"]);
    meta.breaks = fabric_map(&v["breaks"]).into_iter().map(|(k, r)| (k, r, true)).chain(fabric_map(&v["conflicts"]).into_iter().map(|(k, r)| (k, r, false))).collect();
    v["jars"].as_array().map(|a| a.iter().filter_map(|j| j["file"].as_str().map(String::from)).collect()).unwrap_or_default()
}

fn parse_quilt(text: &str, meta: &mut Meta) -> Vec<String> {
    let Ok(v) = serde_json::from_str::<serde_json::Value>(text) else { return vec![] };
    let q = &v["quilt_loader"];
    meta.id = q["id"].as_str().unwrap_or_default().to_string();
    meta.name = q["metadata"]["name"].as_str().unwrap_or(&meta.id).to_string();
    meta.version = q["version"].as_str().unwrap_or_default().to_string();
    let entry = |d: &serde_json::Value| -> Option<(String, Range, bool)> {
        match d {
            serde_json::Value::String(s) => Some((s.clone(), Range::Fabric(vec!["*".into()]), false)),
            serde_json::Value::Object(_) => {
                let range = d.get("versions").map(fabric_range).unwrap_or(Range::Fabric(vec!["*".into()]));
                Some((d["id"].as_str()?.to_string(), range, d["optional"].as_bool().unwrap_or(false)))
            }
            _ => None,
        }
    };
    let deps = q["depends"].as_array().cloned().unwrap_or_default();
    meta.depends = deps.iter().filter_map(entry).filter(|(_, _, optional)| !optional).map(|(id, r, _)| (id, r)).collect();
    let breaks = q["breaks"].as_array().cloned().unwrap_or_default();
    meta.breaks = breaks.iter().filter_map(entry).map(|(id, r, _)| (id, r, true)).collect();
    q["jars"].as_array().map(|a| a.iter().filter_map(|s| s.as_str().map(String::from)).collect()).unwrap_or_default()
}

fn parse_toml(text: &str, meta: &mut Meta) {
    let mut table = String::new();
    let mut tables: Vec<(String, HashMap<String, String>)> = vec![];
    let mut in_multiline = false;
    for line in text.lines() {
        let l = line.trim();
        let quotes = l.matches("'''").count() + l.matches("\"\"\"").count();
        if in_multiline {
            in_multiline = quotes % 2 == 0;
            continue;
        }
        if l.starts_with('#') || l.is_empty() {
            continue;
        }
        if l.starts_with('[') {
            table = l.split('#').next().unwrap_or(l).trim().trim_matches(['[', ']']).trim().to_string();
            tables.push((table.clone(), HashMap::new()));
            continue;
        }
        let Some((k, v)) = l.split_once('=') else { continue };
        if quotes % 2 == 1 {
            in_multiline = true;
        }
        let v = v.split(" #").next().unwrap_or(v).trim().trim_matches(['"', '\'']).to_string();
        if let Some((_, t)) = tables.last_mut().filter(|(t, _)| *t == table) {
            t.insert(k.trim().to_string(), v);
        }
    }
    for (name, t) in &tables {
        let get = |k: &str| t.get(k).cloned().unwrap_or_default();
        if name == "mods" && meta.id.is_empty() {
            meta.id = get("modId");
            meta.name = t.get("displayName").cloned().unwrap_or_else(|| meta.id.clone());
            meta.version = get("version");
        } else if name.starts_with("dependencies") {
            let id = get("modId");
            if id.is_empty() || get("side") == "SERVER" {
                continue;
            }
            let range = Range::Maven(get("versionRange"));
            let kind = t.get("type").map(|s| s.to_lowercase());
            match kind.as_deref() {
                Some("required") => meta.depends.push((id, range)),
                Some("incompatible") => meta.breaks.push((id, range, true)),
                Some("discouraged") => meta.breaks.push((id, range, false)),
                Some(_) => {}
                None if get("mandatory") == "true" => meta.depends.push((id, range)),
                None => {}
            }
        }
    }
}

fn read_meta<R: Read + Seek>(zip: &mut zip::ZipArchive<R>, prefer: Family, depth: u32) -> Meta {
    let mut found: Vec<(Family, Meta, Vec<String>)> = vec![];
    if let Some(t) = read_text(zip, "fabric.mod.json") {
        let mut m = Meta::default();
        let jars = parse_fabric(&t, &mut m);
        found.push((Family::Fabric, m, jars));
    }
    if let Some(t) = read_text(zip, "quilt.mod.json") {
        let mut m = Meta::default();
        let jars = parse_quilt(&t, &mut m);
        found.push((Family::Quilt, m, jars));
    }
    if let Some(t) = read_text(zip, "META-INF/neoforge.mods.toml").or_else(|| read_text(zip, "META-INF/mods.toml")) {
        let mut m = Meta::default();
        parse_toml(&t, &mut m);
        found.push((Family::Forge, m, vec![]));
    }
    let families: Vec<Family> = found.iter().map(|(f, _, _)| *f).collect();
    let pick = found.iter().position(|(f, _, _)| *f == prefer).or_else(|| found.iter().position(|(f, _, _)| prefer == Family::Quilt && *f == Family::Fabric)).unwrap_or(0);
    let (mut meta, mut jars) = if found.is_empty() { (Meta::default(), vec![]) } else { let (_, m, j) = found.swap_remove(pick); (m, j) };
    meta.families = families;
    if meta.families.is_empty() && zip.by_name("mcmod.info").is_ok() {
        meta.families.push(Family::Forge);
    }
    if depth < 2 {
        let names: Vec<String> =
            zip.file_names().filter_map(|n| n.ok().map(|n| n.into_owned())).filter(|n| n.starts_with("META-INF/jarjar/") && n.ends_with(".jar")).collect();
        jars.extend(names);
        for j in jars {
            let Ok(mut f) = zip.by_name(&j) else { continue };
            if f.size() > NESTED_MAX {
                continue;
            }
            let mut bytes = Vec::new();
            if f.read_to_end(&mut bytes).is_err() {
                continue;
            }
            drop(f);
            if let Ok(mut inner) = zip::ZipArchive::new(Cursor::new(bytes)) {
                let m = read_meta(&mut inner, prefer, depth + 1);
                if !m.id.is_empty() {
                    meta.nested.push(m);
                }
            }
        }
    }
    meta
}

fn parse_ver(v: &str) -> Option<Vec<u64>> {
    let core = v.trim().split(['-', '+']).next()?;
    core.split('.').map(|p| p.parse().ok()).collect()
}

fn parse_maven(v: &str) -> Option<Vec<u64>> {
    let core = v.trim().split('+').next()?;
    core.split(['.', '-']).map(|p| p.parse().ok()).collect()
}

fn cmp_ver(a: &[u64], b: &[u64]) -> std::cmp::Ordering {
    let n = a.len().max(b.len());
    (0..n).map(|i| a.get(i).unwrap_or(&0).cmp(b.get(i).unwrap_or(&0))).find(|o| o.is_ne()).unwrap_or(std::cmp::Ordering::Equal)
}

fn fabric_term(term: &str, v: &[u64]) -> Option<bool> {
    use std::cmp::Ordering::*;
    let term = term.trim();
    if term.is_empty() || term == "*" {
        return Some(true);
    }
    let (op, rest) = [">=", "<=", ">", "<", "=", "~", "^"].iter().find_map(|op| term.strip_prefix(op).map(|r| (*op, r.trim()))).unwrap_or(("", term));
    if rest.split(['-', '+']).next()?.split('.').any(|p| matches!(p, "x" | "X" | "*")) {
        let prefix: Vec<u64> = rest.split('.').take_while(|p| !matches!(*p, "x" | "X" | "*")).map(|p| p.parse().ok()).collect::<Option<_>>()?;
        return Some(prefix.iter().enumerate().all(|(i, p)| v.get(i).copied().unwrap_or(0) == *p));
    }
    let w = parse_ver(rest)?;
    let o = cmp_ver(v, &w);
    Some(match op {
        ">=" => o != Less,
        "<=" => o != Greater,
        ">" => o == Greater,
        "<" => o == Less,
        "~" => {
            let mut upper = w[..w.len().min(2)].to_vec();
            if upper.len() == 1 {
                upper[0] += 1;
            } else {
                upper[1] += 1;
            }
            o != Less && cmp_ver(v, &upper) == Less
        }
        "^" => o != Less && cmp_ver(v, &[w[0] + 1]) == Less,
        _ => o == Equal,
    })
}

fn maven(range: &str, version: &str) -> Option<bool> {
    use std::cmp::Ordering::*;
    let r = range.trim();
    if r.is_empty() || r == "*" || !r.starts_with(['[', '(']) {
        return Some(true);
    }
    let v = &parse_maven(version)?;
    let mut any = false;
    let mut rest = r;
    while let Some(start) = rest.find(['[', '(']) {
        let end = rest[start..].find([']', ')'])? + start;
        let (open, close) = (&rest[start..=start], &rest[end..=end]);
        let body = &rest[start + 1..end];
        let ok = match body.split_once(',') {
            None => cmp_ver(v, &parse_maven(body)?) == Equal,
            Some((lo, hi)) => {
                let lo_ok = lo.trim().is_empty() || {
                    let o = cmp_ver(v, &parse_maven(lo)?);
                    if open == "[" { o != Less } else { o == Greater }
                };
                let hi_ok = hi.trim().is_empty() || {
                    let o = cmp_ver(v, &parse_maven(hi)?);
                    if close == "]" { o != Greater } else { o == Less }
                };
                lo_ok && hi_ok
            }
        };
        any |= ok;
        rest = &rest[end + 1..];
    }
    Some(any)
}

fn satisfies(range: &Range, version: &str) -> Option<bool> {
    if let Range::Maven(r) = range {
        return maven(r, version);
    }
    let v = parse_ver(version)?;
    match range {
        Range::Fabric(alts) if alts.is_empty() => Some(true),
        Range::Fabric(alts) => {
            let results: Vec<Option<bool>> = alts.iter().map(|a| a.split_whitespace().map(|t| fabric_term(t, &v)).try_fold(true, |acc, r| r.map(|r| acc && r))).collect();
            if results.contains(&Some(true)) { Some(true) } else if results.iter().all(|r| r.is_some()) { Some(false) } else { None }
        }
        Range::Maven(_) => None,
    }
}

fn range_text(r: &Range) -> String {
    match r {
        Range::Fabric(a) => a.join(" || "),
        Range::Maven(s) => s.clone(),
    }
}

fn check(metas: &[(String, Meta)], mc: &str, loader: Option<LoaderKind>) -> Vec<Conflict> {
    let mut out = vec![];
    let mut present: HashMap<String, Vec<(String, String)>> = HashMap::new();
    fn add(present: &mut HashMap<String, Vec<(String, String)>>, m: &Meta, file: &str) {
        for id in std::iter::once(&m.id).chain(&m.provides) {
            present.entry(id.clone()).or_default().push((m.version.clone(), file.to_string()));
        }
        for n in &m.nested {
            add(present, n, file);
        }
    }
    for (file, m) in metas {
        add(&mut present, m, file);
    }
    let name_of = |id: &str| metas.iter().find(|(_, m)| m.id == id).map(|(_, m)| m.name.clone());

    let mut top: HashMap<&str, Vec<&str>> = HashMap::new();
    for (file, m) in metas.iter().filter(|(_, m)| !m.id.is_empty()) {
        top.entry(m.id.as_str()).or_default().push(file);
    }
    let mut dups: Vec<_> = top.into_iter().filter(|(_, f)| f.len() > 1).collect();
    dups.sort();
    for (id, files) in dups {
        out.push(Conflict { severity: "error", kind: "duplicate", id: id.into(), name: name_of(id), files: files.iter().map(|f| f.to_string()).collect(), wanted: None, found: None });
    }

    let fabric_like = matches!(loader, Some(LoaderKind::Fabric | LoaderKind::LegacyFabric | LoaderKind::Ornithe));
    let quilt = loader == Some(LoaderKind::Quilt);
    let forge_like = matches!(loader, Some(LoaderKind::Forge | LoaderKind::NeoForge));
    let connector = present.contains_key("connectormod");
    for (file, m) in metas {
        let fits = m.families.is_empty()
            || (fabric_like && m.families.contains(&Family::Fabric))
            || (quilt && (m.families.contains(&Family::Fabric) || m.families.contains(&Family::Quilt)))
            || (forge_like && (m.families.contains(&Family::Forge) || connector && m.families.contains(&Family::Fabric)))
            || loader.is_none();
        if !fits {
            let found = m.families.iter().map(|f| format!("{f:?}")).collect::<Vec<_>>().join(", ");
            out.push(Conflict { severity: "error", kind: "loader", id: m.id.clone(), name: Some(m.name.clone()), files: vec![file.clone()], wanted: None, found: Some(found) });
            continue;
        }
        for (dep, range) in &m.depends {
            if dep == "minecraft" {
                if satisfies(range, mc) == Some(false) {
                    out.push(Conflict { severity: "error", kind: "minecraft", id: m.id.clone(), name: Some(m.name.clone()), files: vec![file.clone()], wanted: Some(range_text(range)), found: Some(mc.into()) });
                }
                continue;
            }
            if BUILTIN.contains(&dep.as_str()) {
                continue;
            }
            let alias = if dep == "fabric" { "fabric_api".to_string() } else { dep.replace('-', "_") };
            match present.get(dep).or_else(|| present.get(&alias)) {
                None => out.push(Conflict { severity: "error", kind: "missing", id: dep.clone(), name: None, files: vec![file.clone()], wanted: Some(range_text(range)), found: None }),
                Some(have) => {
                    let results: Vec<Option<bool>> = have.iter().map(|(v, _)| satisfies(range, v)).collect();
                    if !results.is_empty() && results.iter().all(|r| *r == Some(false)) {
                        let found = have.iter().map(|(v, _)| v.clone()).collect::<Vec<_>>().join(", ");
                        out.push(Conflict { severity: "warning", kind: "depVersion", id: dep.clone(), name: name_of(dep), files: vec![file.clone()], wanted: Some(range_text(range)), found: Some(found) });
                    }
                }
            }
        }
        for (other, range, hard) in &m.breaks {
            let Some(have) = present.get(other) else { continue };
            let hits: Vec<&(String, String)> = have.iter().filter(|(v, f)| f != file && satisfies(range, v) != Some(false)).collect();
            if hits.is_empty() {
                continue;
            }
            let mut files = vec![file.clone()];
            files.extend(hits.iter().map(|(_, f)| f.clone()).filter(|f| f != file));
            files.dedup();
            let severity = if *hard { "error" } else { "warning" };
            out.push(Conflict { severity, kind: "breaks", id: other.clone(), name: name_of(other), files, wanted: Some(m.name.clone()), found: Some(hits[0].0.clone()) });
        }
    }
    out.dedup();
    out
}

pub async fn scan(instance_id: &str) -> Result<Vec<Conflict>, String> {
    let inst = instances::get(instance_id)?;
    let dir = instances::game_dir(instance_id)?.join("mods");
    let files: Vec<String> = list(instance_id)?.into_iter().filter(|f| f.folder == "mods" && f.enabled && !f.file.ends_with(DISABLED)).map(|f| f.file).collect();
    let prefer = match inst.loader.as_ref().map(|l| l.kind) {
        Some(LoaderKind::Forge | LoaderKind::NeoForge) => Family::Forge,
        Some(LoaderKind::Quilt) => Family::Quilt,
        _ => Family::Fabric,
    };
    tokio::task::spawn_blocking(move || {
        let metas: Vec<(String, Meta)> = files
            .into_iter()
            .filter_map(|f| {
                let mut zip = zip::ZipArchive::new(std::fs::File::open(dir.join(&f)).ok()?).ok()?;
                Some((f, read_meta(&mut zip, prefer, 0)))
            })
            .collect();
        check(&metas, &inst.mc_version, inst.loader.map(|l| l.kind))
    })
    .await
    .map_err(|e| e.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn fab(alts: &[&str]) -> Range {
        Range::Fabric(alts.iter().map(|s| s.to_string()).collect())
    }

    #[test]
    fn version_ranges() {
        assert_eq!(satisfies(&fab(&["~1.20.1"]), "1.20.4"), Some(true));
        assert_eq!(satisfies(&fab(&["~1.20.1"]), "1.21"), Some(false));
        assert_eq!(satisfies(&fab(&[">=1.20 <1.21"]), "1.20.6"), Some(true));
        assert_eq!(satisfies(&fab(&["1.20.x"]), "1.20.1"), Some(true));
        assert_eq!(satisfies(&fab(&["1.20.x"]), "1.21.1"), Some(false));
        assert_eq!(satisfies(&fab(&["1.19.4", "1.20.1"]), "1.20.1"), Some(true));
        assert_eq!(satisfies(&fab(&[">=1.21.2-alpha.24.38.a"]), "1.21.4"), Some(true));
        assert_eq!(satisfies(&fab(&["*"]), "26.1.2"), Some(true));
        assert_eq!(satisfies(&fab(&["1.20.1"]), "24w14a"), None);
        assert_eq!(satisfies(&Range::Maven("[1.20.1,1.21)".into()), "1.20.1"), Some(true));
        assert_eq!(satisfies(&Range::Maven("[1.20.1,1.21)".into()), "1.21"), Some(false));
        assert_eq!(satisfies(&Range::Maven("[1.18,1.19),[1.20,)".into()), "1.20.1"), Some(true));
        assert_eq!(satisfies(&Range::Maven("[1.18,1.19),[1.20,)".into()), "1.19.2"), Some(false));
        assert_eq!(satisfies(&Range::Maven("[47,)".into()), "47.2.0"), Some(true));
        assert_eq!(satisfies(&Range::Maven("1.0".into()), "0.1"), Some(true));
        assert_eq!(satisfies(&Range::Maven("[1.20.1-4,1.20.1-5)".into()), "1.20.1-4.18.2"), Some(true));
    }

    #[test]
    fn reads_mods_toml() {
        let mut m = Meta::default();
        parse_toml(
            "modLoader=\"javafml\"\n[[mods]] #mandatory\nmodId=\"fzzy\" #mandatory\ndisplayName=\"Fzzy\"\nversion=\"0.7.6\"\ndescription='''\n[[not a table]]\n'''\n\
             [[dependencies.fzzy]]\nmodId=\"kotlinforforge\"\nmandatory=true\nversionRange=\"[4,)\"\nside=\"BOTH\"\n\
             [[dependencies.fzzy]]\nmodId=\"jei\"\ntype=\"optional\"\n[[dependencies.fzzy]]\nmodId=\"optifine\"\ntype=\"incompatible\"\n",
            &mut m,
        );
        assert_eq!((m.id.as_str(), m.name.as_str(), m.version.as_str()), ("fzzy", "Fzzy", "0.7.6"));
        assert_eq!(m.depends, vec![("kotlinforforge".to_string(), Range::Maven("[4,)".into()))]);
        assert_eq!(m.breaks.len(), 1);
    }

    fn meta(id: &str, fam: Family, deps: &[(&str, &str)]) -> Meta {
        Meta {
            id: id.into(),
            name: id.into(),
            version: "1.0.0".into(),
            families: vec![fam],
            depends: deps.iter().map(|(d, r)| (d.to_string(), fab(&[r]))).collect(),
            ..Default::default()
        }
    }

    #[test]
    fn finds_conflicts() {
        let mut api = meta("fabric-api", Family::Fabric, &[]);
        api.nested.push(meta("fabric-api-base", Family::Fabric, &[]));
        let mut sodium = meta("sodium", Family::Fabric, &[("minecraft", "~1.20.1")]);
        sodium.breaks.push(("optifabric".into(), fab(&["*"]), true));
        let metas = vec![
            ("api.jar".to_string(), api),
            ("a.jar".to_string(), meta("a", Family::Fabric, &[("fabric-api-base", "*"), ("fabricloader", ">=0.15"), ("kotlin", "*")])),
            ("sodium.jar".to_string(), sodium),
            ("sodium-old.jar".to_string(), meta("sodium", Family::Fabric, &[])),
            ("of.jar".to_string(), meta("optifabric", Family::Fabric, &[])),
            ("forge.jar".to_string(), meta("f", Family::Forge, &[])),
        ];
        let found = check(&metas, "1.21.1", Some(LoaderKind::Fabric));
        let kinds: Vec<(&str, &str)> = found.iter().map(|c| (c.kind, c.id.as_str())).collect();
        assert!(kinds.contains(&("duplicate", "sodium")), "{kinds:?}");
        assert!(kinds.contains(&("missing", "kotlin")), "{kinds:?}");
        assert!(kinds.contains(&("minecraft", "sodium")), "{kinds:?}");
        assert!(kinds.contains(&("breaks", "optifabric")), "{kinds:?}");
        assert!(kinds.contains(&("loader", "f")), "{kinds:?}");
        assert!(!kinds.iter().any(|(_, id)| *id == "fabric-api-base" || *id == "fabricloader"), "{kinds:?}");
    }
}
