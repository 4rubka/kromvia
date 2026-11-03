
use std::path::{Path, PathBuf};
use std::sync::{Arc, Mutex};
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use serde::{Deserialize, Serialize};
use serde_json::json;

use crate::engine::core::http;
use crate::engine::core::jobs::Job;
use crate::engine::core::paths;
use crate::engine::skins;

const MS_AUTH: &str = "https://login.microsoftonline.com/consumers/oauth2/v2.0";
const SCOPE: &str = "XboxLive.signin offline_access";
const MC_API: &str = "https://api.minecraftservices.com";
const EXPIRY_MARGIN: u64 = 300;

fn client_id() -> Result<String, String> {
    std::env::var("KROMVIA_MS_CLIENT_ID")
        .ok()
        .or(option_env!("KROMVIA_MS_CLIENT_ID").map(String::from))
        .filter(|s| !s.is_empty())
        .ok_or_else(|| "Microsoft sign-in is not configured in this build (no client id)".into())
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
struct Account {
    id: String,
    name: String,
    refresh_token: String,
    access_token: String,
    expires_at: u64,
    #[serde(default)]
    xuid: String,
    #[serde(default, skip_serializing_if = "String::is_empty")]
    provider: String,
    #[serde(default, skip_serializing_if = "String::is_empty")]
    client_token: String,
}

const ELYBY: &str = "elyby";
const ELY_AUTH: &str = "https://authserver.ely.by/auth";
pub const ELY_INJECTOR_API: &str = "https://authserver.ely.by/api/authlib-injector";

#[derive(Serialize, Deserialize, Default)]
struct Store {
    active: Option<String>,
    accounts: Vec<Account>,
}

#[derive(Serialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct AccountInfo {
    pub id: String,
    pub name: String,
    pub active: bool,
    pub provider: &'static str,
}

pub struct Session {
    pub name: String,
    pub uuid: String,
    pub access_token: String,
    pub xuid: String,
    pub online: bool,
    pub injector: Option<&'static str>,
}

static STORE_LOCK: Mutex<()> = Mutex::new(());

fn store_path() -> PathBuf {
    paths::root().join("accounts.json")
}

fn read_store(path: &Path) -> Store {
    std::fs::read(path).ok().and_then(|b| serde_json::from_slice(&b).ok()).unwrap_or_default()
}

fn write_store(path: &Path, store: &Store) -> Result<(), String> {
    let json = serde_json::to_vec_pretty(store).map_err(|e| e.to_string())?;
    let tmp = path.with_extension("json.tmp");
    let mut opts = std::fs::OpenOptions::new();
    opts.write(true).create(true).truncate(true);
    #[cfg(unix)]
    std::os::unix::fs::OpenOptionsExt::mode(&mut opts, 0o600);
    std::io::Write::write_all(&mut opts.open(&tmp).map_err(|e| e.to_string())?, &json).map_err(|e| e.to_string())?;
    std::fs::rename(&tmp, path).map_err(|e| e.to_string())
}

fn edit<T>(f: impl FnOnce(&mut Store) -> Result<T, String>) -> Result<T, String> {
    let _guard = STORE_LOCK.lock().unwrap();
    let path = store_path();
    let mut store = read_store(&path);
    let out = f(&mut store)?;
    write_store(&path, &store)?;
    Ok(out)
}

pub fn list() -> Vec<AccountInfo> {
    let store = read_store(&store_path());
    store
        .accounts
        .iter()
        .map(|a| AccountInfo {
            id: a.id.clone(),
            name: a.name.clone(),
            active: store.active.as_ref() == Some(&a.id),
            provider: if a.provider == ELYBY { "elyby" } else { "microsoft" },
        })
        .collect()
}

pub fn set_active(id: Option<&str>) -> Result<(), String> {
    edit(|s| {
        if let Some(id) = id {
            if !s.accounts.iter().any(|a| a.id == id) {
                return Err(format!("no account {id}"));
            }
        }
        s.active = id.map(String::from);
        Ok(())
    })
}

pub fn remove(id: &str) -> Result<(), String> {
    edit(|s| {
        s.accounts.retain(|a| a.id != id);
        if s.active.as_deref() == Some(id) {
            s.active = s.accounts.first().map(|a| a.id.clone());
        }
        Ok(())
    })
}

fn now() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_secs()).unwrap_or(0)
}

pub async fn session(id: &str) -> Result<Session, String> {
    let acc = read_store(&store_path()).accounts.into_iter().find(|a| a.id == id).ok_or("account was removed")?;
    if acc.provider == ELYBY {
        return ely_session(acc).await;
    }
    let acc = if acc.expires_at > now() + EXPIRY_MARGIN {
        acc
    } else {
        let ms = refresh_ms(&acc.refresh_token).await.map_err(|e| format!("{}: sign in again ({e})", acc.name))?;
        let fresh = minecraft_from_ms(ms).await?;
        save_account(fresh.clone(), false)?;
        fresh
    };
    Ok(Session { name: acc.name, uuid: acc.id, access_token: acc.access_token, xuid: acc.xuid, online: true, injector: None })
}

async fn ms_token(id: &str) -> Result<String, String> {
    let s = session(id).await?;
    if s.injector.is_some() {
        return Err("Ely.by skins and capes are changed on ely.by".into());
    }
    Ok(s.access_token)
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct YggProfile {
    id: String,
    name: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct YggAuth {
    access_token: String,
    client_token: String,
    selected_profile: Option<YggProfile>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct YggError {
    #[serde(default)]
    error_message: String,
}

async fn ygg_post<T: serde::de::DeserializeOwned>(endpoint: &str, body: serde_json::Value) -> Result<Option<T>, String> {
    let url = format!("{ELY_AUTH}/{endpoint}");
    let resp = http::client().post(&url).json(&body).send().await.map_err(|e| format!("Ely.by: {e}"))?;
    let status = resp.status();
    if status.as_u16() == 204 {
        return Ok(None);
    }
    let text = resp.text().await.map_err(|e| format!("Ely.by: {e}"))?;
    if !status.is_success() {
        let msg = serde_json::from_str::<YggError>(&text).map(|e| e.error_message).unwrap_or_default();
        return Err(if msg.is_empty() { format!("Ely.by: HTTP {status}") } else { format!("Ely.by: {msg}") });
    }
    if text.trim().is_empty() {
        return Ok(None);
    }
    serde_json::from_str(&text).map(Some).map_err(|e| format!("Ely.by: {e}"))
}

fn new_client_token() -> Result<String, String> {
    let mut b = [0u8; 16];
    getrandom::fill(&mut b).map_err(|e| e.to_string())?;
    Ok(b.iter().map(|x| format!("{x:02x}")).collect())
}

pub async fn login_elyby(username: &str, password: &str, totp: Option<&str>) -> Result<AccountInfo, String> {
    let username = username.trim();
    if username.is_empty() || password.is_empty() {
        return Err("enter your Ely.by login and password".into());
    }
    let password = match totp.map(str::trim).filter(|t| !t.is_empty()) {
        Some(code) if code.len() <= 8 && code.bytes().all(|b| b.is_ascii_digit()) => format!("{password}:{code}"),
        Some(_) => return Err("the code is 6 digits".into()),
        None => password.to_string(),
    };
    let client_token = new_client_token()?;
    let auth: YggAuth = ygg_post("authenticate", json!({ "username": username, "password": password, "clientToken": client_token, "requestUser": true }))
        .await?
        .ok_or("Ely.by sent an empty answer")?;
    let profile = auth.selected_profile.ok_or("this Ely.by account has no Minecraft profile")?;
    if profile.id.len() != 32 || !profile.id.bytes().all(|b| b.is_ascii_hexdigit()) {
        return Err("Ely.by returned a bad profile id".into());
    }
    let acc = Account {
        id: profile.id,
        name: profile.name,
        refresh_token: String::new(),
        access_token: auth.access_token,
        expires_at: 0,
        xuid: String::new(),
        provider: ELYBY.into(),
        client_token: auth.client_token,
    };
    let info = AccountInfo { id: acc.id.clone(), name: acc.name.clone(), active: true, provider: "elyby" };
    save_account(acc, true)?;
    Ok(info)
}

async fn ely_session(mut acc: Account) -> Result<Session, String> {
    let valid = ygg_post::<serde_json::Value>("validate", json!({ "accessToken": acc.access_token })).await.is_ok();
    if !valid {
        let auth: YggAuth = ygg_post("refresh", json!({ "accessToken": acc.access_token, "clientToken": acc.client_token, "requestUser": true }))
            .await
            .map_err(|e| format!("{}: sign in again ({e})", acc.name))?
            .ok_or("Ely.by sent an empty answer")?;
        acc.access_token = auth.access_token;
        if let Some(p) = auth.selected_profile {
            acc.name = p.name;
        }
        save_account(acc.clone(), false)?;
    }
    Ok(Session { name: acc.name, uuid: acc.id, access_token: acc.access_token, xuid: "0".into(), online: true, injector: Some(ELY_INJECTOR_API) })
}

fn save_account(acc: Account, make_active: bool) -> Result<(), String> {
    edit(|s| {
        s.accounts.retain(|a| a.id != acc.id);
        if make_active || s.active.is_none() {
            s.active = Some(acc.id.clone());
        }
        s.accounts.push(acc);
        Ok(())
    })
}

#[derive(Deserialize, Serialize, Clone, Debug)]
#[serde(rename_all = "camelCase")]
pub struct DeviceCode {
    #[serde(alias = "user_code")]
    pub user_code: String,
    #[serde(alias = "verification_uri")]
    pub verification_uri: String,
    #[serde(alias = "expires_in")]
    pub expires_in: u64,
    #[serde(skip_serializing, default, alias = "device_code")]
    device_code: String,
    #[serde(skip_serializing, default)]
    interval: u64,
}

#[derive(Deserialize)]
struct MsToken {
    access_token: String,
    refresh_token: String,
}

#[derive(Deserialize)]
struct OAuthError {
    error: String,
    #[serde(default)]
    error_description: String,
}

async fn post_form(url: &str, form: &[(&str, &str)]) -> Result<Result<MsToken, OAuthError>, String> {
    let body = form
        .iter()
        .map(|(k, v)| format!("{}={}", urlencode(k), urlencode(v)))
        .collect::<Vec<_>>()
        .join("&");
    let resp = http::client()
        .post(url)
        .header("content-type", "application/x-www-form-urlencoded")
        .body(body)
        .send()
        .await
        .map_err(|e| format!("{url}: {e}"))?;
    let ok = resp.status().is_success();
    let text = resp.text().await.map_err(|e| format!("{url}: {e}"))?;
    if ok {
        serde_json::from_str(&text).map(Ok).map_err(|e| format!("{url}: {e}"))
    } else {
        serde_json::from_str(&text).map(Err).map_err(|_| format!("{url}: {text}"))
    }
}

fn urlencode(s: &str) -> String {
    s.bytes()
        .map(|b| match b {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'.' | b'_' | b'~' => (b as char).to_string(),
            _ => format!("%{b:02X}"),
        })
        .collect()
}

pub async fn start_login() -> Result<DeviceCode, String> {
    let cid = client_id()?;
    let url = format!("{MS_AUTH}/devicecode");
    let body = format!("client_id={}&scope={}", urlencode(&cid), urlencode(SCOPE));
    let resp = http::client()
        .post(&url)
        .header("content-type", "application/x-www-form-urlencoded")
        .body(body)
        .send()
        .await
        .map_err(|e| format!("{url}: {e}"))?;
    if !resp.status().is_success() {
        return Err(format!("{url}: HTTP {}", resp.status()));
    }
    resp.json().await.map_err(|e| format!("{url}: {e}"))
}

pub async fn finish_login(code: DeviceCode, job: Arc<Job>) -> Result<(), String> {
    let cid = client_id()?;
    let url = format!("{MS_AUTH}/token");
    let mut interval = code.interval.max(1);
    let deadline = now() + code.expires_in;
    job.progress("signin", 0, None);
    let ms = loop {
        tokio::time::sleep(Duration::from_secs(interval)).await;
        if job.is_cancelled() {
            return Err("cancelled".into());
        }
        if now() > deadline {
            return Err("the code expired, try again".into());
        }
        let form = [
            ("grant_type", "urn:ietf:params:oauth:grant-type:device_code"),
            ("client_id", cid.as_str()),
            ("device_code", code.device_code.as_str()),
        ];
        match post_form(&url, &form).await? {
            Ok(token) => break token,
            Err(e) if e.error == "authorization_pending" => {}
            Err(e) if e.error == "slow_down" => interval += 5,
            Err(e) if e.error == "authorization_declined" => return Err("sign-in was declined".into()),
            Err(e) if e.error == "expired_token" => return Err("the code expired, try again".into()),
            Err(e) => return Err(format!("{}: {}", e.error, e.error_description)),
        }
    };
    job.progress("xbox", 0, None);
    let acc = minecraft_from_ms(ms).await?;
    save_account(acc, true)
}

async fn refresh_ms(refresh_token: &str) -> Result<MsToken, String> {
    let cid = client_id()?;
    let form = [("grant_type", "refresh_token"), ("client_id", cid.as_str()), ("refresh_token", refresh_token), ("scope", SCOPE)];
    post_form(&format!("{MS_AUTH}/token"), &form).await?.map_err(|e| e.error)
}

#[derive(Deserialize)]
#[serde(rename_all = "PascalCase")]
struct XboxToken {
    token: String,
    display_claims: XboxClaims,
}

#[derive(Deserialize)]
struct XboxClaims {
    xui: Vec<XboxUser>,
}

#[derive(Deserialize)]
struct XboxUser {
    uhs: String,
    #[serde(default)]
    xid: Option<String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "PascalCase")]
struct XstsError {
    x_err: u64,
}

#[derive(Deserialize)]
struct McLogin {
    access_token: String,
    expires_in: u64,
}

#[derive(Deserialize, Serialize, Clone, Debug)]
pub struct Profile {
    pub id: String,
    pub name: String,
    #[serde(default)]
    pub skins: Vec<Skin>,
    #[serde(default)]
    pub capes: Vec<Cape>,
}

#[derive(Deserialize, Serialize, Clone, Debug)]
pub struct Skin {
    pub id: String,
    pub state: String,
    pub url: String,
    #[serde(default)]
    pub variant: String,
}

#[derive(Deserialize, Serialize, Clone, Debug)]
pub struct Cape {
    pub id: String,
    pub state: String,
    pub url: String,
    #[serde(default)]
    pub alias: String,
}

fn xsts_message(code: u64) -> String {
    match code {
        2148916233 => "this Microsoft account has no Xbox profile; sign in once at minecraft.net".into(),
        2148916235 => "Xbox Live is not available in your country".into(),
        2148916236 | 2148916237 => "this account needs adult verification on xbox.com".into(),
        2148916238 => "this is a child account; an adult must add it to a Microsoft family".into(),
        other => format!("Xbox Live refused the sign-in (XErr {other})"),
    }
}

async fn post_json<T: serde::de::DeserializeOwned>(url: &str, body: &serde_json::Value) -> Result<T, (u16, String)> {
    let resp = http::client()
        .post(url)
        .header("accept", "application/json")
        .json(body)
        .send()
        .await
        .map_err(|e| (0, format!("{url}: {e}")))?;
    let status = resp.status();
    let text = resp.text().await.map_err(|e| (0, format!("{url}: {e}")))?;
    if !status.is_success() {
        return Err((status.as_u16(), text));
    }
    serde_json::from_str(&text).map_err(|e| (0, format!("{url}: {e}")))
}

async fn minecraft_from_ms(ms: MsToken) -> Result<Account, String> {
    let xbl: XboxToken = post_json(
        "https://user.auth.xboxlive.com/user/authenticate",
        &json!({
            "Properties": { "AuthMethod": "RPS", "SiteName": "user.auth.xboxlive.com", "RpsTicket": format!("d={}", ms.access_token) },
            "RelyingParty": "http://auth.xboxlive.com",
            "TokenType": "JWT",
        }),
    )
    .await
    .map_err(|(s, e)| format!("Xbox Live sign-in failed ({s}): {e}"))?;

    let xsts: XboxToken = post_json(
        "https://xsts.auth.xboxlive.com/xsts/authorize",
        &json!({
            "Properties": { "SandboxId": "RETAIL", "UserTokens": [xbl.token] },
            "RelyingParty": "rp://api.minecraftservices.com/",
            "TokenType": "JWT",
        }),
    )
    .await
    .map_err(|(s, body)| match serde_json::from_str::<XstsError>(&body) {
        Ok(e) => xsts_message(e.x_err),
        Err(_) => format!("XSTS failed ({s}): {body}"),
    })?;
    let user = xsts.display_claims.xui.first().ok_or("XSTS returned no user")?;

    let mc: McLogin = post_json(
        &format!("{MC_API}/authentication/login_with_xbox"),
        &json!({ "identityToken": format!("XBL3.0 x={};{}", user.uhs, xsts.token) }),
    )
    .await
    .map_err(|(s, e)| match s {
        403 => "Minecraft refused this launcher's sign-in (the app is not approved by Mojang yet)".to_string(),
        _ => format!("Minecraft sign-in failed ({s}): {e}"),
    })?;

    let profile = get_profile(&mc.access_token).await?;
    Ok(Account {
        id: profile.id,
        name: profile.name,
        refresh_token: ms.refresh_token,
        access_token: mc.access_token,
        expires_at: now() + mc.expires_in,
        xuid: user.xid.clone().unwrap_or_default(),
        provider: String::new(),
        client_token: String::new(),
    })
}

async fn get_profile(token: &str) -> Result<Profile, String> {
    let url = format!("{MC_API}/minecraft/profile");
    let resp = http::client().get(&url).bearer_auth(token).send().await.map_err(|e| format!("{url}: {e}"))?;
    match resp.status().as_u16() {
        200 => resp.json().await.map_err(|e| format!("{url}: {e}")),
        404 => Err("this account does not own Minecraft: Java Edition".into()),
        s => Err(format!("{url}: HTTP {s}")),
    }
}

pub async fn profile(id: &str) -> Result<Profile, String> {
    get_profile(&ms_token(id).await?).await
}

async fn send(req: reqwest::RequestBuilder, what: &str) -> Result<Profile, String> {
    let resp = req.send().await.map_err(|e| format!("{what}: {e}"))?;
    let status = resp.status();
    if !status.is_success() {
        let body = resp.text().await.unwrap_or_default();
        return Err(format!("{what}: HTTP {status} {body}"));
    }
    resp.json().await.map_err(|e| format!("{what}: {e}"))
}

pub async fn upload_skin(id: &str, png: Vec<u8>, slim: bool) -> Result<Profile, String> {
    skins::check_png(&png)?;
    let token = ms_token(id).await?;
    let part = reqwest::multipart::Part::bytes(png).file_name("skin.png").mime_str("image/png").map_err(|e| e.to_string())?;
    let form = reqwest::multipart::Form::new().text("variant", if slim { "slim" } else { "classic" }).part("file", part);
    send(http::client().post(format!("{MC_API}/minecraft/profile/skins")).bearer_auth(token).multipart(form), "skin upload").await
}

pub async fn reset_skin(id: &str) -> Result<Profile, String> {
    let token = ms_token(id).await?;
    send(http::client().delete(format!("{MC_API}/minecraft/profile/skins/active")).bearer_auth(token), "skin reset").await
}

pub async fn set_cape(id: &str, cape: Option<&str>) -> Result<Profile, String> {
    let token = ms_token(id).await?;
    let url = format!("{MC_API}/minecraft/profile/capes/active");
    let req = match cape {
        Some(cape) => http::client().put(url).json(&json!({ "capeId": cape })),
        None => http::client().delete(url),
    };
    send(req.bearer_auth(token), "cape").await
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn encodes_form_values() {
        assert_eq!(urlencode("XboxLive.signin offline_access"), "XboxLive.signin%20offline_access");
        assert_eq!(urlencode("a=b&c"), "a%3Db%26c");
    }

    #[test]
    fn parses_device_code() {
        let c: DeviceCode = serde_json::from_str(
            r#"{"user_code":"ABCD1234","device_code":"dc","verification_uri":"https://www.microsoft.com/link","expires_in":900,"interval":5,"message":"x"}"#,
        )
        .unwrap();
        assert_eq!((c.user_code.as_str(), c.device_code.as_str(), c.interval), ("ABCD1234", "dc", 5));
        let out = serde_json::to_string(&c).unwrap();
        assert!(!out.contains("\"dc\"") && out.contains("userCode"));
    }

    #[test]
    fn parses_xbox_replies() {
        let x: XboxToken = serde_json::from_str(r#"{"Token":"t","DisplayClaims":{"xui":[{"uhs":"123","xid":"999"}]}}"#).unwrap();
        assert_eq!((x.token.as_str(), x.display_claims.xui[0].uhs.as_str()), ("t", "123"));
        let e: XstsError = serde_json::from_str(r#"{"Identity":"0","XErr":2148916238,"Message":""}"#).unwrap();
        assert!(xsts_message(e.x_err).contains("child"));
    }

    #[test]
    fn store_round_trip_hides_tokens() {
        let path = std::env::temp_dir().join(format!("kromvia-acc-{}.json", std::process::id()));
        let acc = |id: &str| Account {
            id: id.into(),
            name: format!("n{id}"),
            refresh_token: "secret".into(),
            access_token: "secret".into(),
            expires_at: 0,
            xuid: String::new(),
            provider: String::new(),
            client_token: String::new(),
        };
        let store = Store { active: Some("a".into()), accounts: vec![acc("a"), acc("b")] };
        write_store(&path, &store).unwrap();
        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            assert_eq!(std::fs::metadata(&path).unwrap().permissions().mode() & 0o777, 0o600);
        }
        let back = read_store(&path);
        assert_eq!(back.accounts.len(), 2);
        let info = AccountInfo { id: "a".into(), name: "na".into(), active: true, provider: "microsoft" };
        assert!(!serde_json::to_string(&info).unwrap().contains("secret"));
        std::fs::remove_file(path).unwrap();
    }

    #[tokio::test]
    #[ignore]
    async fn elyby_rejects_bad_credentials() {
        let e = login_elyby("kromvia_probe_no_such_user", "wrong", None).await.unwrap_err();
        assert!(e.starts_with("Ely.by: ") && e.contains("Invalid credentials"), "{e}");
        assert!(login_elyby("x", "y", Some("12ab")).await.is_err());
    }
}
