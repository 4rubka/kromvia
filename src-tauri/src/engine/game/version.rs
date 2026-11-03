
use std::collections::{HashMap, HashSet};

use serde::{Deserialize, Serialize};

#[derive(Deserialize, Serialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct VersionJson {
    pub id: String,
    #[serde(default)]
    pub inherits_from: Option<String>,
    pub main_class: String,
    #[serde(default)]
    pub arguments: Option<Arguments>,
    #[serde(default)]
    pub minecraft_arguments: Option<String>,
    #[serde(default)]
    pub libraries: Vec<Library>,
    #[serde(default)]
    pub downloads: Option<VersionDownloads>,
    #[serde(default)]
    pub asset_index: Option<AssetIndexRef>,
    #[serde(default)]
    pub java_version: Option<JavaVersion>,
    #[serde(default)]
    pub logging: Option<Logging>,
    #[serde(rename = "type", default)]
    pub kind: Option<String>,
}

#[derive(Deserialize, Serialize, Clone, Debug, Default)]
pub struct Arguments {
    #[serde(default)]
    pub game: Vec<Argument>,
    #[serde(default)]
    pub jvm: Vec<Argument>,
}

#[derive(Deserialize, Serialize, Clone, Debug)]
#[serde(untagged)]
pub enum Argument {
    Plain(String),
    Ruled { rules: Vec<Rule>, value: OneOrMany },
}

#[derive(Deserialize, Serialize, Clone, Debug)]
#[serde(untagged)]
pub enum OneOrMany {
    One(String),
    Many(Vec<String>),
}

#[derive(Deserialize, Serialize, Clone, Debug)]
pub struct Rule {
    pub action: String,
    #[serde(default)]
    pub os: Option<OsRule>,
    #[serde(default)]
    pub features: Option<HashMap<String, bool>>,
}

#[derive(Deserialize, Serialize, Clone, Debug)]
pub struct OsRule {
    pub name: Option<String>,
    pub arch: Option<String>,
    pub version: Option<String>,
}

#[derive(Deserialize, Serialize, Clone, Debug)]
pub struct Library {
    pub name: String,
    #[serde(default)]
    pub downloads: Option<LibraryDownloads>,
    #[serde(default)]
    pub url: Option<String>,
    #[serde(default)]
    pub sha1: Option<String>,
    #[serde(default)]
    pub size: Option<u64>,
    #[serde(default)]
    pub natives: Option<HashMap<String, String>>,
    #[serde(default)]
    pub rules: Vec<Rule>,
}

#[derive(Deserialize, Serialize, Clone, Debug)]
pub struct LibraryDownloads {
    #[serde(default)]
    pub artifact: Option<Artifact>,
    #[serde(default)]
    pub classifiers: Option<HashMap<String, Artifact>>,
}

#[derive(Deserialize, Serialize, Clone, Debug)]
pub struct Artifact {
    #[serde(default)]
    pub path: Option<String>,
    pub url: String,
    #[serde(default)]
    pub sha1: Option<String>,
    #[serde(default)]
    pub size: Option<u64>,
}

#[derive(Deserialize, Serialize, Clone, Debug)]
pub struct VersionDownloads {
    pub client: Artifact,
}

#[derive(Deserialize, Serialize, Clone, Debug)]
pub struct AssetIndexRef {
    pub id: String,
    pub url: String,
    pub sha1: String,
    pub size: u64,
}

#[derive(Deserialize, Serialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct JavaVersion {
    pub major_version: u32,
}

#[derive(Deserialize, Serialize, Clone, Debug)]
pub struct Logging {
    #[serde(default)]
    pub client: Option<LoggingClient>,
}

#[derive(Deserialize, Serialize, Clone, Debug)]
pub struct LoggingClient {
    pub argument: String,
    pub file: LogConfigFile,
}

#[derive(Deserialize, Serialize, Clone, Debug)]
pub struct LogConfigFile {
    pub id: String,
    pub url: String,
    pub sha1: String,
    pub size: u64,
}

pub fn check_id(id: &str) -> Result<(), String> {
    let ok = !id.is_empty()
        && id.len() <= 64
        && !id.starts_with('.')
        && id.chars().all(|c| c.is_ascii_alphanumeric() || "._+- ".contains(c));
    if ok { Ok(()) } else { Err(format!("invalid version id: {id}")) }
}

pub fn url_segment(id: &str) -> String {
    id.replace(' ', "%20")
}

pub fn os_name() -> &'static str {
    match std::env::consts::OS {
        "windows" => "windows",
        "macos" => "osx",
        _ => "linux",
    }
}

fn os_arch() -> &'static str {
    match std::env::consts::ARCH {
        "x86" => "x86",
        "aarch64" => "arm64",
        _ => "x86_64",
    }
}

pub fn rules_allow(rules: &[Rule]) -> bool {
    if rules.is_empty() {
        return true;
    }
    let mut allowed = false;
    for rule in rules {
        if rule_matches(rule) {
            allowed = rule.action == "allow";
        }
    }
    allowed
}

fn rule_matches(rule: &Rule) -> bool {
    if let Some(os) = &rule.os {
        if os.name.as_deref().is_some_and(|n| n != os_name())
            || os.arch.as_deref().is_some_and(|a| a != os_arch())
            || os.version.is_some()
        {
            return false;
        }
    }
    if let Some(features) = &rule.features {
        if features.values().any(|&on| on) {
            return false;
        }
    }
    true
}

pub fn flatten_args(args: &[Argument]) -> Vec<String> {
    let mut out = Vec::new();
    for arg in args {
        match arg {
            Argument::Plain(s) => out.push(s.clone()),
            Argument::Ruled { rules, value } if rules_allow(rules) => match value {
                OneOrMany::One(s) => out.push(s.clone()),
                OneOrMany::Many(v) => out.extend(v.iter().cloned()),
            },
            Argument::Ruled { .. } => {}
        }
    }
    out
}

pub fn maven_path(coord: &str) -> Option<String> {
    let (coord, ext) = coord.split_once('@').unwrap_or((coord, "jar"));
    let parts: Vec<&str> = coord.split(':').collect();
    let (group, artifact, version, classifier) = match parts[..] {
        [g, a, v] => (g, a, v, None),
        [g, a, v, c] => (g, a, v, Some(c)),
        _ => return None,
    };
    let file = match classifier {
        Some(c) => format!("{artifact}-{version}-{c}.{ext}"),
        None => format!("{artifact}-{version}.{ext}"),
    };
    Some(format!("{}/{artifact}/{version}/{file}", group.replace('.', "/")))
}

fn library_key(name: &str) -> String {
    let parts: Vec<&str> = name.split(':').collect();
    match parts[..] {
        [g, a, _, c, ..] => format!("{g}:{a}:{c}"),
        [g, a, ..] => format!("{g}:{a}"),
        _ => name.to_string(),
    }
}

impl Library {
    pub fn artifact(&self) -> Option<Artifact> {
        if let Some(a) = self.downloads.as_ref().and_then(|d| d.artifact.clone()) {
            return Some(Artifact { path: a.path.or_else(|| maven_path(&self.name)), ..a });
        }
        if self.downloads.is_some() || self.natives.is_some() {
            return None;
        }
        let path = maven_path(&self.name)?;
        let base = self.url.as_deref().unwrap_or("https://libraries.minecraft.net/");
        let base = base.strip_suffix('/').unwrap_or(base);
        Some(Artifact { url: format!("{base}/{path}"), path: Some(path), sha1: self.sha1.clone(), size: self.size })
    }

    pub fn native_artifact(&self) -> Option<Artifact> {
        let classifier = self.natives.as_ref()?.get(os_name())?.replace(
            "${arch}",
            if std::env::consts::ARCH == "x86" { "32" } else { "64" },
        );
        self.downloads.as_ref()?.classifiers.as_ref()?.get(&classifier).cloned()
    }
}

const LF_MAVEN: &str = "https://maven.legacyfabric.net";
const LF_LWJGL: &str = "2.9.4+legacyfabric.17";
const LF_LWJGL_FILES: [(&str, Option<&str>, &str, u64); 3] = [
    ("lwjgl", None, "b0d3b134274c82aa401c90b42f0572063b1bf735", 1081524),
    ("lwjgl_util", None, "60b2bc63267f55fda5295d470e61d9fc672fa14c", 180575),
    ("lwjgl-platform", Some("natives-osx"), "720009ad4fb3ebc510d2452e97dec3b0c6b4182f", 1599360),
];

pub fn swap_lwjgl2(libraries: Vec<Library>) -> Vec<Library> {
    let is_lwjgl2 = |name: &str| {
        let mut parts = name.split(':');
        parts.next() == Some("org.lwjgl.lwjgl")
            && parts.next().is_some_and(|a| ["lwjgl", "lwjgl_util", "lwjgl-platform"].contains(&a))
            && parts.next().is_some_and(|v| v.starts_with("2."))
    };
    if !libraries.iter().any(|l| is_lwjgl2(&l.name)) {
        return libraries;
    }
    let mut out: Vec<Library> = libraries.into_iter().filter(|l| !is_lwjgl2(&l.name)).collect();
    for (artifact, classifier, sha1, size) in LF_LWJGL_FILES {
        let name = format!("org.lwjgl.lwjgl:{artifact}:{LF_LWJGL}");
        let coord = classifier.map_or(name.clone(), |c| format!("{name}:{c}"));
        let path = maven_path(&coord).expect("valid coordinate");
        let file = Artifact { url: format!("{LF_MAVEN}/{path}"), path: Some(path), sha1: Some(sha1.into()), size: Some(size) };
        let (downloads, natives) = match classifier {
            Some(c) => (
                LibraryDownloads { artifact: None, classifiers: Some(HashMap::from([(c.to_string(), file)])) },
                Some(HashMap::from([("osx".to_string(), c.to_string())])),
            ),
            None => (LibraryDownloads { artifact: Some(file), classifiers: None }, None),
        };
        out.push(Library { name, downloads: Some(downloads), url: None, sha1: None, size: None, natives, rules: vec![] });
    }
    out
}

pub fn merge(child: VersionJson, parent: VersionJson) -> VersionJson {
    let mut seen = HashSet::new();
    let libraries = child
        .libraries
        .into_iter()
        .chain(parent.libraries)
        .filter(|l| !rules_allow(&l.rules) || seen.insert(format!("{}{}", library_key(&l.name), if l.natives.is_some() { ":legacy-natives" } else { "" })))
        .collect();
    let arguments = match (parent.arguments, child.arguments) {
        (Some(mut p), Some(c)) => {
            p.game.extend(c.game);
            p.jvm.extend(c.jvm);
            Some(p)
        }
        (p, c) => c.or(p),
    };
    VersionJson {
        id: child.id,
        inherits_from: None,
        main_class: child.main_class,
        arguments,
        minecraft_arguments: child.minecraft_arguments.or(parent.minecraft_arguments),
        libraries,
        downloads: child.downloads.or(parent.downloads),
        asset_index: child.asset_index.or(parent.asset_index),
        java_version: child.java_version.or(parent.java_version),
        logging: child.logging.or(parent.logging),
        kind: parent.kind.or(child.kind),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn swaps_lwjgl2_for_arm_macs() {
        let lib = |name: &str| Library { name: name.into(), downloads: None, url: None, sha1: None, size: None, natives: None, rules: vec![] };
        let libs = vec![lib("org.lwjgl.lwjgl:lwjgl:2.9.4-nightly-20150209"), lib("org.lwjgl.lwjgl:lwjgl-platform:2.9.2-nightly-20140822"), lib("com.paulscode:codecjorbis:20101023")];
        let out = swap_lwjgl2(libs);
        let names: Vec<&str> = out.iter().map(|l| l.name.as_str()).collect();
        assert_eq!(names, ["com.paulscode:codecjorbis:20101023", "org.lwjgl.lwjgl:lwjgl:2.9.4+legacyfabric.17", "org.lwjgl.lwjgl:lwjgl_util:2.9.4+legacyfabric.17", "org.lwjgl.lwjgl:lwjgl-platform:2.9.4+legacyfabric.17"]);
        let natives = out[3].downloads.as_ref().unwrap().classifiers.as_ref().unwrap().get("natives-osx").unwrap();
        assert_eq!(natives.url, "https://maven.legacyfabric.net/org/lwjgl/lwjgl/lwjgl-platform/2.9.4+legacyfabric.17/lwjgl-platform-2.9.4+legacyfabric.17-natives-osx.jar");
        let modern = vec![lib("org.lwjgl:lwjgl:3.3.3")];
        assert_eq!(swap_lwjgl2(modern).len(), 1);
    }

    fn rule(json: &str) -> Rule {
        serde_json::from_str(json).unwrap()
    }

    #[test]
    fn evaluates_rules() {
        let here = os_name();
        let other = if here == "windows" { "linux" } else { "windows" };
        assert!(rules_allow(&[]));
        assert!(rules_allow(&[rule(&format!(r#"{{"action":"allow","os":{{"name":"{here}"}}}}"#))]));
        assert!(!rules_allow(&[rule(&format!(r#"{{"action":"allow","os":{{"name":"{other}"}}}}"#))]));
        assert!(rules_allow(&[
            rule(r#"{"action":"allow"}"#),
            rule(&format!(r#"{{"action":"disallow","os":{{"name":"{other}"}}}}"#)),
        ]));
        assert!(!rules_allow(&[rule(r#"{"action":"allow","features":{"is_demo_user":true}}"#)]));
        assert!(!rules_allow(&[rule(&format!(r#"{{"action":"allow","os":{{"name":"{here}","version":"^10\\."}}}}"#))]));
    }

    #[test]
    fn flattens_arguments() {
        let args: Vec<Argument> = serde_json::from_str(
            r#"["--username", "${auth_player_name}",
                {"rules":[{"action":"allow","features":{"has_custom_resolution":true}}],"value":["--width","1"]},
                {"rules":[{"action":"allow"}],"value":"-Xss1M"}]"#,
        )
        .unwrap();
        assert_eq!(flatten_args(&args), ["--username", "${auth_player_name}", "-Xss1M"]);
    }

    #[test]
    fn checks_ids() {
        for ok in ["1.21.4", "26.4-snapshot-5", "1.14 Pre-Release 1", "0.16.9+build.1"] {
            assert!(check_id(ok).is_ok(), "{ok}");
        }
        for bad in ["", "..", "../x", "1.21/../../x", "a?b", "a#b", "a%2F"] {
            assert!(check_id(bad).is_err(), "{bad}");
        }
        assert_eq!(url_segment("1.14 Pre-Release 1"), "1.14%20Pre-Release%201");
    }

    #[test]
    fn builds_maven_paths() {
        assert_eq!(maven_path("net.fabricmc:fabric-loader:0.16.9").unwrap(), "net/fabricmc/fabric-loader/0.16.9/fabric-loader-0.16.9.jar");
        assert_eq!(maven_path("org.lwjgl:lwjgl:3.3.3:natives-linux").unwrap(), "org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives-linux.jar");
        assert!(maven_path("broken").is_none());
    }

    #[test]
    fn merge_prefers_child_libraries() {
        let parent: VersionJson = serde_json::from_str(
            r#"{"id":"1.21","mainClass":"net.minecraft.client.main.Main","type":"release",
                "libraries":[{"name":"org.ow2.asm:asm:9.0"},{"name":"org.lwjgl:lwjgl:3.3.3"},{"name":"org.lwjgl:lwjgl:3.3.3:natives-linux"}],
                "arguments":{"game":["--a"],"jvm":["-cp"]}}"#,
        )
        .unwrap();
        let child: VersionJson = serde_json::from_str(
            r#"{"id":"fabric","inheritsFrom":"1.21","mainClass":"net.fabricmc.loader.impl.launch.knot.KnotClient",
                "libraries":[{"name":"org.ow2.asm:asm:9.7","url":"https://maven.fabricmc.net/"}],
                "arguments":{"game":[],"jvm":["-DFabricMcEmu=x"]}}"#,
        )
        .unwrap();
        let m = merge(child, parent);
        let names: Vec<&str> = m.libraries.iter().map(|l| l.name.as_str()).collect();
        assert_eq!(names, ["org.ow2.asm:asm:9.7", "org.lwjgl:lwjgl:3.3.3", "org.lwjgl:lwjgl:3.3.3:natives-linux"]);
        assert_eq!(m.main_class, "net.fabricmc.loader.impl.launch.knot.KnotClient");
        assert_eq!(flatten_args(&m.arguments.unwrap().jvm), ["-cp", "-DFabricMcEmu=x"]);
        assert_eq!(m.kind.as_deref(), Some("release"));
        assert_eq!(m.libraries[0].artifact().unwrap().url, "https://maven.fabricmc.net/org/ow2/asm/asm/9.7/asm-9.7.jar");
    }
}
