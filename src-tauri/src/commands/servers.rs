use crate::engine::game::launch;
use crate::engine::servers::{self, ServerStatus};

#[tauri::command]
pub async fn server_status(address: String) -> Result<ServerStatus, String> {
    if !launch::valid_address(&address) {
        return Err("bad server address".into());
    }
    servers::status(&address).await
}
