use tauri::{AppHandle, State};

use crate::engine::accounts::{self, AccountInfo, DeviceCode, Profile};
use crate::engine::core::jobs::Jobs;
use crate::engine::skins::{self, FoundSkin, LibrarySkin, SkinView};

#[derive(serde::Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LoginStart {
    code: DeviceCode,
    job_id: u64,
}

#[tauri::command]
pub fn account_list() -> Vec<AccountInfo> {
    accounts::list()
}

#[tauri::command]
pub async fn account_login(app: AppHandle, jobs: State<'_, Jobs>) -> Result<LoginStart, String> {
    let code = accounts::start_login().await?;
    let pending = code.clone();
    let job_id = jobs.spawn(&app, "account-login".into(), move |job| accounts::finish_login(pending, job))?;
    Ok(LoginStart { code, job_id })
}

#[tauri::command]
pub async fn account_login_elyby(username: String, password: String, totp: Option<String>) -> Result<AccountInfo, String> {
    accounts::login_elyby(&username, &password, totp.as_deref()).await
}

#[tauri::command]
pub fn account_select(id: Option<String>) -> Result<(), String> {
    accounts::set_active(id.as_deref())
}

#[tauri::command]
pub fn account_remove(id: String) -> Result<(), String> {
    accounts::remove(&id)
}

#[tauri::command]
pub async fn account_profile(id: String) -> Result<Profile, String> {
    let profile = accounts::profile(&id).await?;
    remember(&profile).await;
    Ok(profile)
}

async fn remember(profile: &Profile) {
    if let Some(skin) = profile.skins.iter().find(|s| s.state == "ACTIVE") {
        if let Ok(png) = skins::download_texture(&skin.url).await {
            let _ = skins::add(&png, &profile.name, skin.variant == "SLIM");
        }
    }
}

#[tauri::command]
pub async fn skin_wear(id: String, skin_id: String) -> Result<Profile, String> {
    let (skin, png) = skins::get(&skin_id)?;
    accounts::upload_skin(&id, png, skin.slim).await
}

#[tauri::command]
pub fn wardrobe_list() -> Vec<SkinView> {
    skins::list()
}

#[tauri::command]
pub fn wardrobe_add_file(path: String, slim: bool) -> Result<LibrarySkin, String> {
    let path = std::path::Path::new(&path);
    if std::fs::metadata(path).map_err(|e| e.to_string())?.len() > 64 * 1024 {
        return Err("skin file is too big".into());
    }
    let png = std::fs::read(path).map_err(|e| e.to_string())?;
    let name = path.file_stem().and_then(|s| s.to_str()).unwrap_or("Skin");
    skins::add(&png, name, slim)
}

#[tauri::command]
pub fn wardrobe_add_data(data_url: String, name: String, slim: bool) -> Result<LibrarySkin, String> {
    skins::add_from_data_url(&data_url, &name, slim)
}

#[tauri::command]
pub async fn wardrobe_add_nick(nick: String) -> Result<LibrarySkin, String> {
    skins::add_from_nick(&nick).await
}

#[tauri::command]
pub fn wardrobe_remove(skin_id: String) -> Result<(), String> {
    skins::remove(&skin_id)
}

#[tauri::command]
pub fn wardrobe_rename(skin_id: String, name: String) -> Result<(), String> {
    skins::rename(&skin_id, &name)
}

#[tauri::command]
pub async fn skin_lookup(nick: String) -> Result<FoundSkin, String> {
    skins::lookup(&nick).await
}

#[tauri::command]
pub async fn optifine_cape(nick: String) -> Result<Option<String>, String> {
    skins::optifine_cape(&nick).await
}

#[tauri::command]
pub async fn skin_reset(id: String) -> Result<Profile, String> {
    accounts::reset_skin(&id).await
}

#[tauri::command]
pub async fn cape_select(id: String, cape: Option<String>) -> Result<Profile, String> {
    accounts::set_cape(&id, cape.as_deref()).await
}
