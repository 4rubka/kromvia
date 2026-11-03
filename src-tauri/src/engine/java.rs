use std::collections::HashSet;
use std::path::{Path, PathBuf};
use std::process::Stdio;
use std::sync::Arc;
use std::time::Duration;

use futures_util::future::join_all;
use serde::{Deserialize, Serialize};
use tokio::process::Command;

use crate::engine::core::http::{self, Download, Hash};
use crate::engine::core::jobs::Job;
use crate::engine::core::paths;

pub const SUPPORTED_MAJORS: [u32; 4] = [8, 17, 21, 25];

const PROBE_TIMEOUT: Duration = Duration::from_secs(10);

#[derive(Serialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct JavaRuntime {
    pub path: PathBuf,
    pub major: u32,
    pub version: String,
    pub is_64bit: bool,
    pub managed: bool,
}

pub async fn list() -> Vec<JavaRuntime> {
    let mut seen = HashSet::new();
    let mut candidates = Vec::new();
    for (bin, managed) in managed_bins().into_iter().map(|b| (b, true)).chain(system_bins().into_iter().map(|b| (b, false))) {
        let key = std::fs::canonicalize(&bin).unwrap_or_else(|_| bin.clone());
        if seen.insert(key) {
            candidates.push((bin, managed));
        }
    }
    join_all(candidates.into_iter().map(|(bin, managed)| async move {
        probe(&bin).await.ok().map(|mut rt| {
            rt.managed = managed;
            rt
        })
    }))
    .await
    .into_iter()
    .flatten()
    .collect()
}

pub async fn check(path: &str) -> Result<JavaRuntime, String> {
    let path = std::fs::canonicalize(path).map_err(|e| e.to_string())?;
    let name = path.file_name().and_then(|s| s.to_str()).unwrap_or_default();
    if name != exe("java") && name != exe("javaw") {
        return Err("not a java executable".into());
    }
    let home = path.parent().filter(|d| d.ends_with("bin")).and_then(Path::parent);
    if !home.is_some_and(|h| h.join("release").is_file()) {
        return Err("not inside a Java installation".into());
    }
    probe(&path.with_file_name(exe("java"))).await
}

pub fn remove(major: u32) -> Result<(), String> {
    let dir = managed_dir(major);
    if !dir.exists() {
        return Err(format!("Java {major} is not installed"));
    }
    std::fs::remove_dir_all(dir).map_err(|e| e.to_string())
}

pub fn managed_bin(major: u32) -> Option<PathBuf> {
    Some(bin_in_home(&managed_dir(major))).filter(|b| b.is_file())
}

pub fn pick_major(required: u32) -> Result<u32, String> {
    SUPPORTED_MAJORS
        .into_iter()
        .find(|&m| m >= required)
        .ok_or_else(|| format!("this version needs Java {required}, which Kromvia cannot install yet"))
}

fn managed_dir(major: u32) -> PathBuf {
    paths::runtimes().join(format!("java-{major}"))
}

fn managed_bins() -> Vec<PathBuf> {
    SUPPORTED_MAJORS.iter().map(|&m| bin_in_home(&managed_dir(m))).filter(|b| b.is_file()).collect()
}

fn exe(name: &str) -> String {
    if cfg!(windows) { format!("{name}.exe") } else { name.to_string() }
}

fn bin_in_home(home: &Path) -> PathBuf {
    let mac_home = home.join("Contents").join("Home");
    let home = if mac_home.is_dir() { mac_home } else { home.to_path_buf() };
    home.join("bin").join(exe("java"))
}

fn system_bins() -> Vec<PathBuf> {
    let mut homes: Vec<PathBuf> = Vec::new();
    if let Some(h) = std::env::var_os("JAVA_HOME") {
        homes.push(h.into());
    }
    let roots: &[&str] = if cfg!(windows) {
        &[
            r"C:\Program Files\Java",
            r"C:\Program Files\Eclipse Adoptium",
            r"C:\Program Files\Zulu",
            r"C:\Program Files\Microsoft",
            r"C:\Program Files\BellSoft",
        ]
    } else if cfg!(target_os = "macos") {
        &["/Library/Java/JavaVirtualMachines"]
    } else {
        &["/usr/lib/jvm", "/usr/lib64/jvm", "/usr/java", "/opt/java"]
    };
    for root in roots {
        if let Ok(entries) = std::fs::read_dir(root) {
            homes.extend(entries.flatten().map(|e| e.path()));
        }
    }

    let mut bins: Vec<PathBuf> = homes.iter().map(|h| bin_in_home(h)).collect();
    if let Some(path) = std::env::var_os("PATH") {
        bins.extend(std::env::split_paths(&path).map(|d| d.join(exe("java"))));
    }
    bins.retain(|b| b.is_file());
    bins
}

async fn probe(bin: &Path) -> Result<JavaRuntime, String> {
    let mut cmd = Command::new(bin);
    cmd.arg("-version").stdin(Stdio::null()).stdout(Stdio::null()).stderr(Stdio::piped()).kill_on_drop(true);
    #[cfg(windows)]
    cmd.creation_flags(0x0800_0000); // CREATE_NO_WINDOW

    let out = tokio::time::timeout(PROBE_TIMEOUT, cmd.output())
        .await
        .map_err(|_| "java -version timed out".to_string())?
        .map_err(|e| e.to_string())?;
    if !out.status.success() {
        return Err(format!("java -version exited with {}", out.status));
    }
    let stderr = String::from_utf8_lossy(&out.stderr);
    let version = parse_version(&stderr).ok_or("unrecognized java -version output")?;
    Ok(JavaRuntime {
        path: bin.to_path_buf(),
        major: parse_major(&version).ok_or("unrecognized java version")?,
        version,
        is_64bit: stderr.contains("64-Bit"),
        managed: false,
    })
}

fn parse_version(stderr: &str) -> Option<String> {
    let line = stderr.lines().find(|l| l.contains(" version \""))?;
    let start = line.find('"')? + 1;
    let end = start + line[start..].find('"')?;
    Some(line[start..end].to_string())
}

fn parse_major(version: &str) -> Option<u32> {
    let mut parts = version.split(|c: char| !c.is_ascii_digit());
    match parts.next()?.parse().ok()? {
        1 => parts.next()?.parse().ok(),
        n => Some(n),
    }
}

#[derive(Deserialize)]
struct AdoptiumAsset {
    binary: AdoptiumBinary,
}

#[derive(Deserialize)]
struct AdoptiumBinary {
    package: AdoptiumPackage,
}

#[derive(Deserialize)]
struct AdoptiumPackage {
    link: String,
    name: String,
    checksum: String,
    size: u64,
}

#[derive(Deserialize)]
struct AzulListed {
    package_uuid: String,
}

#[derive(Deserialize)]
struct AzulPackage {
    download_url: String,
    name: String,
    sha256_hash: String,
}

struct Package {
    link: String,
    name: String,
    checksum: String,
    size: Option<u64>,
}

async fn adoptium_package(major: u32) -> Result<Package, String> {
    let (os, arch) = adoptium_target()?;
    let url = format!(
        "https://api.adoptium.net/v3/assets/latest/{major}/hotspot?architecture={arch}&image_type=jre&os={os}&vendor=eclipse"
    );
    let assets: Vec<AdoptiumAsset> = http::get_json(&url).await?;
    let pkg = assets
        .into_iter()
        .next()
        .ok_or_else(|| format!("no Temurin {major} build for {os}/{arch}"))?
        .binary
        .package;
    Ok(Package { link: pkg.link, name: pkg.name, checksum: pkg.checksum, size: Some(pkg.size) })
}

async fn azul_package(major: u32) -> Result<Package, String> {
    let (os, arch) = match (std::env::consts::OS, std::env::consts::ARCH) {
        ("windows", "x86_64") => ("windows", "x64"),
        ("windows", "aarch64") => ("windows", "aarch64"),
        ("macos", "x86_64") => ("macos", "x64"),
        ("macos", "aarch64") => ("macos", "aarch64"),
        ("linux", "x86_64") => ("linux", "x64"),
        ("linux", "aarch64") => ("linux", "aarch64"),
        (os, arch) => return Err(format!("no Azul build for {os}/{arch}")),
    };
    let ext = if os == "windows" { "zip" } else { "tar.gz" };
    let url = format!(
        "https://api.azul.com/metadata/v1/zulu/packages/?java_version={major}&os={os}&arch={arch}&archive_type={ext}\
         &java_package_type=jre&javafx_bundled=false&latest=true&release_status=ga&availability_types=CA&page_size=1"
    );
    let listed: Vec<AzulListed> = http::get_json(&url).await?;
    let id = listed.into_iter().next().ok_or_else(|| format!("no Zulu {major} build for {os}/{arch}"))?.package_uuid;
    check_uuid(&id)?;
    let pkg: AzulPackage = http::get_json(&format!("https://api.azul.com/metadata/v1/zulu/packages/{id}")).await?;
    Ok(Package { link: pkg.download_url, name: pkg.name, checksum: pkg.sha256_hash, size: None })
}

fn check_uuid(id: &str) -> Result<(), String> {
    if id.len() == 36 && id.chars().all(|c| c.is_ascii_hexdigit() || c == '-') {
        Ok(())
    } else {
        Err("unexpected package id from Azul".into())
    }
}

async fn resolve_package(major: u32) -> Result<Package, String> {
    match adoptium_package(major).await {
        Ok(pkg) => Ok(pkg),
        Err(adoptium) => azul_package(major).await.map_err(|azul| format!("{adoptium}; {azul}")),
    }
}

fn adoptium_target() -> Result<(&'static str, &'static str), String> {
    let os = match std::env::consts::OS {
        "windows" => "windows",
        "macos" => "mac",
        "linux" => "linux",
        other => return Err(format!("unsupported OS: {other}")),
    };
    let arch = match std::env::consts::ARCH {
        "x86_64" => "x64",
        "aarch64" => "aarch64",
        "x86" => "x32",
        other => return Err(format!("unsupported architecture: {other}")),
    };
    Ok((os, arch))
}

pub async fn install(major: u32, job: Arc<Job>) -> Result<(), String> {
    if !SUPPORTED_MAJORS.contains(&major) {
        return Err(format!("Java {major} is not supported"));
    }
    job.progress("resolve", 0, None);
    let pkg = resolve_package(major).await?;
    if !pkg.link.starts_with("https://") || pkg.name.contains(['/', '\\']) {
        return Err("unexpected Java package".into());
    }

    let archive = paths::temp().join(&pkg.name);
    http::download(
        &Download { url: &pkg.link, dest: &archive, hash: Some(Hash::Sha256(pkg.checksum)), size: pkg.size },
        job.cancel_flag(),
        |done, total| job.progress("download", done, total),
    )
    .await?;

    job.progress("extract", 0, None);
    let staging = paths::runtimes().join(format!(".staging-java-{major}"));
    let _ = std::fs::remove_dir_all(&staging);
    let unpack = {
        let (archive, staging) = (archive.clone(), staging.clone());
        tokio::task::spawn_blocking(move || extract(&archive, &staging))
    };
    let result = unpack.await.map_err(|e| e.to_string()).and_then(|r| r);
    let _ = std::fs::remove_file(&archive);
    let finish = async {
        result?;
        let home = single_child_dir(&staging)?;
        let home = java_home(&home).ok_or("no java binary in the archive")?;
        let rt = probe(&bin_in_home(&home)).await?;
        if rt.major != major {
            return Err(format!("expected Java {major}, got {}", rt.major));
        }
        let dest = managed_dir(major);
        let _ = std::fs::remove_dir_all(&dest);
        std::fs::rename(&home, &dest).map_err(|e| e.to_string())
    };
    let result = finish.await;
    let _ = std::fs::remove_dir_all(&staging);
    result
}

fn extract(archive: &Path, dest: &Path) -> Result<(), String> {
    std::fs::create_dir_all(dest).map_err(|e| e.to_string())?;
    let file = std::fs::File::open(archive).map_err(|e| e.to_string())?;
    let name = archive.to_string_lossy();
    if name.ends_with(".zip") {
        zip::ZipArchive::new(file).and_then(|mut z| z.extract(dest)).map_err(|e| e.to_string())
    } else if name.ends_with(".tar.gz") {
        let mut tar = tar::Archive::new(flate2::read::GzDecoder::new(file));
        for entry in tar.entries().map_err(|e| e.to_string())? {
            entry.map_err(|e| e.to_string())?.unpack_in(dest).map_err(|e| e.to_string())?;
        }
        Ok(())
    } else {
        Err(format!("unknown archive type: {name}"))
    }
}

fn java_home(dir: &Path) -> Option<PathBuf> {
    if bin_in_home(dir).is_file() {
        return Some(dir.to_path_buf());
    }
    std::fs::read_dir(dir).ok()?.flatten().map(|e| e.path()).find(|p| p.is_dir() && bin_in_home(p).is_file())
}

fn single_child_dir(dir: &Path) -> Result<PathBuf, String> {
    let mut dirs = std::fs::read_dir(dir).map_err(|e| e.to_string())?.flatten().filter(|e| e.path().is_dir());
    match (dirs.next(), dirs.next()) {
        (Some(only), None) => Ok(only.path()),
        _ => Err("unexpected archive layout".into()),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_versions() {
        let j8 = "openjdk version \"1.8.0_402\"\nOpenJDK Runtime Environment (Temurin)(build 1.8.0_402-b06)\nOpenJDK 64-Bit Server VM";
        let j21 = "openjdk version \"21.0.4\" 2024-07-16 LTS\nOpenJDK 64-Bit Server VM";
        let oracle = "java version \"25\" 2025-09-16 LTS";
        assert_eq!(parse_version(j8).as_deref(), Some("1.8.0_402"));
        assert_eq!(parse_major("1.8.0_402"), Some(8));
        assert_eq!(parse_major(&parse_version(j21).unwrap()), Some(21));
        assert_eq!(parse_major(&parse_version(oracle).unwrap()), Some(25));
        assert_eq!(parse_version("Picked up _JAVA_OPTIONS: -Xmx1g\nopenjdk version \"17.0.2\"").as_deref(), Some("17.0.2"));
        assert_eq!(parse_version("garbage"), None);
    }

    #[tokio::test]
    async fn check_rejects_non_java() {
        assert!(check("/bin/sh").await.is_err());
        let dir = std::env::temp_dir().join(format!("kromvia-fakejava-{}", std::process::id()));
        std::fs::create_dir_all(dir.join("bin")).unwrap();
        std::fs::write(dir.join("bin").join(exe("java")), "").unwrap();
        assert_eq!(check(dir.join("bin").join(exe("java")).to_str().unwrap()).await.unwrap_err(), "not inside a Java installation");
        std::fs::write(dir.join("bin").join("java.sh"), "").unwrap();
        assert!(check(dir.join("bin").join("java.sh").to_str().unwrap()).await.is_err());
        std::fs::remove_dir_all(dir).unwrap();
    }
}

#[cfg(test)]
mod install_tests {
    use super::*;

    #[tokio::test]
    #[ignore]
    async fn azul_resolves_every_major() {
        for major in SUPPORTED_MAJORS {
            let pkg = azul_package(major).await.unwrap();
            assert!(pkg.link.starts_with("https://cdn.azul.com/"), "{}", pkg.link);
            assert_eq!(pkg.checksum.len(), 64, "{}", pkg.name);
        }
    }
    use crate::engine::core::jobs::Jobs;
    use tauri::{Listener, Manager};

    #[tokio::test(flavor = "multi_thread")]
    #[ignore]
    async fn installs_java_21() {
        let root = std::env::temp_dir().join(format!("kromvia-java-{}", std::process::id()));
        paths::init(root.clone());
        let app = tauri::test::mock_builder()
            .manage(Jobs::default())
            .build(tauri::test::mock_context(tauri::test::noop_assets()))
            .unwrap();

        let (tx, rx) = tokio::sync::oneshot::channel::<String>();
        let tx = std::sync::Mutex::new(Some(tx));
        app.listen("job-finished", move |e| {
            if let Some(tx) = tx.lock().unwrap().take() {
                let _ = tx.send(e.payload().to_string());
            }
        });

        let jobs = app.state::<Jobs>();
        jobs.spawn(app.handle(), "java-21".into(), |job| install(21, job)).unwrap();
        assert!(jobs.spawn(app.handle(), "java-21".into(), |job| install(21, job)).is_err(), "double install must be rejected");

        let finished = rx.await.unwrap();
        assert!(finished.contains("\"error\":null"), "{finished}");
        let installed = list().await.into_iter().find(|r| r.managed && r.major == 21);
        assert!(installed.is_some_and(|r| r.is_64bit));
        remove(21).unwrap();
        std::fs::remove_dir_all(root).unwrap();
    }
}
