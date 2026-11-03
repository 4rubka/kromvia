
use std::sync::Arc;

use serde::Deserialize;

use crate::engine::content::{modrinth, ContentKind};
use crate::engine::core::jobs::Job;
use crate::engine::instances;

#[derive(Deserialize, Clone, Copy, Debug, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub enum Preset {
    MaxFps,
    Balanced,
    Quality,
    Auto,
}

impl Preset {
    pub fn for_machine(ram_bytes: u64, threads: usize) -> Preset {
        let gb = ram_bytes / (1024 * 1024 * 1024);
        if gb < 7 || threads <= 4 {
            Preset::MaxFps
        } else if gb >= 15 && threads >= 8 {
            Preset::Quality
        } else {
            Preset::Balanced
        }
    }

    fn mods(self) -> &'static [&'static str] {
        match self {
            Preset::MaxFps => &["sodium", "lithium", "ferrite-core", "entityculling", "immediatelyfast", "dynamic-fps"],
            Preset::Balanced => &["sodium", "lithium", "ferrite-core", "entityculling", "immediatelyfast"],
            Preset::Quality | Preset::Auto => &["sodium", "lithium", "ferrite-core"],
        }
    }

    fn options(self) -> &'static [(&'static str, &'static str)] {
        match self {
            Preset::MaxFps => &[
                ("graphicsPreset", "\"custom\""),
                ("graphicsMode", "0"),
                ("renderDistance", "8"),
                ("simulationDistance", "6"),
                ("particles", "2"),
                ("entityShadows", "false"),
                ("biomeBlendRadius", "0"),
                ("renderClouds", "\"false\""),
                ("mipmapLevels", "0"),
                ("enableVsync", "false"),
                ("maxFps", "260"),
            ],
            Preset::Balanced => &[
                ("graphicsPreset", "\"custom\""),
                ("graphicsMode", "1"),
                ("renderDistance", "12"),
                ("simulationDistance", "8"),
                ("particles", "1"),
                ("entityShadows", "true"),
                ("biomeBlendRadius", "2"),
                ("renderClouds", "\"fast\""),
                ("enableVsync", "false"),
                ("maxFps", "260"),
            ],
            Preset::Quality | Preset::Auto => &[
                ("graphicsPreset", "\"custom\""),
                ("graphicsMode", "1"),
                ("renderDistance", "16"),
                ("simulationDistance", "10"),
                ("particles", "0"),
                ("entityShadows", "true"),
                ("biomeBlendRadius", "5"),
                ("renderClouds", "\"true\""),
            ],
        }
    }
}

fn merge_options(text: &str, values: &[(&str, &str)]) -> String {
    let mut lines: Vec<String> = text.lines().map(String::from).collect();
    for (key, value) in values {
        let line = format!("{key}:{value}");
        match lines.iter_mut().find(|l| l.split_once(':').is_some_and(|(k, _)| k == *key)) {
            Some(existing) => *existing = line,
            None => lines.push(line),
        }
    }
    lines.join("\n") + "\n"
}

pub async fn apply(instance_id: &str, preset: Preset, job: &Arc<Job>) -> Result<Vec<String>, String> {
    let inst = instances::get(instance_id)?;
    let preset = match preset {
        Preset::Auto => Preset::for_machine(super::tuning::total_ram_bytes(), std::thread::available_parallelism().map_or(4, |n| n.get())),
        p => p,
    };
    let mut skipped = Vec::new();
    if inst.loader.is_some() {
        for project in preset.mods() {
            if let Err(e) = modrinth::install(instance_id, project, None, ContentKind::Mod, job).await {
                if job.is_cancelled() {
                    return Err("cancelled".into());
                }
                skipped.push(format!("{project}: {e}"));
            }
        }
    }
    let path = instances::game_dir(instance_id)?.join("options.txt");
    let text = std::fs::read_to_string(&path).unwrap_or_default();
    std::fs::write(&path, merge_options(&text, preset.options())).map_err(|e| e.to_string())?;
    Ok(skipped)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn picks_preset_for_machine() {
        const GB: u64 = 1024 * 1024 * 1024;
        assert_eq!(Preset::for_machine(4 * GB, 8), Preset::MaxFps);
        assert_eq!(Preset::for_machine(32 * GB, 4), Preset::MaxFps);
        assert_eq!(Preset::for_machine(8 * GB, 8), Preset::Balanced);
        assert_eq!(Preset::for_machine(16 * GB, 12), Preset::Quality);
    }

    #[test]
    fn merges_options() {
        let text = "version:4671\nrenderDistance:16\nkey_key.jump:key.keyboard.space\n";
        let out = merge_options(text, &[("renderDistance", "8"), ("maxFps", "260")]);
        assert_eq!(out, "version:4671\nrenderDistance:8\nkey_key.jump:key.keyboard.space\nmaxFps:260\n");
        assert_eq!(merge_options("", &[("a", "1")]), "a:1\n");
    }

    #[test]
    fn every_preset_sets_custom_graphics() {
        for p in [Preset::MaxFps, Preset::Balanced, Preset::Quality] {
            assert!(p.options().contains(&("graphicsPreset", "\"custom\"")));
            assert!(p.mods().contains(&"sodium"));
        }
    }

    #[tokio::test(flavor = "multi_thread")]
    #[ignore]
    async fn applies_real_preset() {
        use crate::engine::core::{jobs::Jobs, paths};
        use crate::engine::game::loaders::{Loader, LoaderKind};
        use tauri::Manager;

        let root = std::env::var("KROMVIA_TEST_ROOT").map(std::path::PathBuf::from).unwrap_or_else(|_| std::env::temp_dir().join("kromvia-it"));
        paths::init(root);
        let app = tauri::test::mock_builder()
            .manage(Jobs::default())
            .build(tauri::test::mock_context(tauri::test::noop_assets()))
            .unwrap();
        let inst = instances::create("preset test", "1.21.11", Some(Loader { kind: LoaderKind::Fabric, version: "0.19.5".into() })).unwrap();
        let id = inst.id.clone();
        let (tx, rx) = tokio::sync::oneshot::channel();
        let tx = std::sync::Mutex::new(Some(tx));
        app.state::<Jobs>()
            .spawn(app.handle(), "preset".into(), move |job| async move {
                let r = apply(&id, Preset::MaxFps, &job).await;
                let _ = tx.lock().unwrap().take().unwrap().send(r);
                Ok(())
            })
            .unwrap();
        let skipped = rx.await.unwrap().unwrap();
        let titles: Vec<String> = crate::engine::content::list(&inst.id).unwrap().into_iter().map(|f| f.title).collect();
        println!("installed {titles:?}, skipped {skipped:?}");
        assert!(titles.iter().any(|t| t == "Sodium"));
        let options = std::fs::read_to_string(instances::game_dir(&inst.id).unwrap().join("options.txt")).unwrap();
        assert!(options.contains("renderDistance:8"));
        instances::delete(&inst.id).unwrap();
    }
}
