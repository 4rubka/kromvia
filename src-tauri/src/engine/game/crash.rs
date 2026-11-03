
use serde::Serialize;

#[derive(Serialize, Clone, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub enum Cause {
    OutOfMemory,
    TooMuchMemory,
    WrongJava,
    MissingMod,
    DuplicateMod,
    BrokenMod,
    Driver,
    Unknown,
}

#[derive(Serialize, Clone, Debug, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct Crash {
    pub cause: Cause,
    pub detail: Option<String>,
    pub report: Option<String>,
    pub auto: bool,
}

const REPORT_MARK: &str = "Crash report saved to: #@!@# ";

pub fn analyze(log: &str, report_path: Option<String>, report: &str) -> Crash {
    let text = format!("{log}\n{report}");
    let line_with = |needle: &str| text.lines().find(|l| l.contains(needle)).map(|l| l.trim().chars().take(300).collect::<String>());
    let any = |needles: &[&str]| needles.iter().any(|n| text.contains(n));

    let (cause, detail) = if any(&["Could not reserve enough space for", "Invalid maximum heap size", "Initial heap size set to a larger value"]) {
        (Cause::TooMuchMemory, None)
    } else if text.contains("java.lang.OutOfMemoryError") {
        (Cause::OutOfMemory, None)
    } else if any(&["UnsupportedClassVersionError", "compiled by a more recent version of the Java Runtime"]) {
        (Cause::WrongJava, line_with("class file version"))
    } else if any(&["Found duplicate mods", "DuplicateModsFoundException", "Duplicate mods found"]) {
        (Cause::DuplicateMod, line_with("Mod ID").or_else(|| line_with("duplicate")))
    } else if let Some(l) = text.lines().find(|l| l.contains("which is missing") || (l.contains("requires") && l.contains("version") && l.trim_start().starts_with("- "))) {
        (Cause::MissingMod, Some(l.trim().trim_start_matches("- ").chars().take(300).collect()))
    } else if text.contains("Missing or unsupported mandatory dependencies") {
        (Cause::MissingMod, line_with("Mod ID:"))
    } else if any(&["Mixin apply failed", "MixinApplyError", "InvalidInjectionException", "InjectionError", "MixinTransformerError"]) {
        (Cause::BrokenMod, mixin_mod(&text))
    } else if any(&["Pixel format not accelerated", "GLFW error 65542", "GLFW error 65543", "does not appear to support OpenGL", "Failed to create OpenGL context", "No OpenGL context"]) {
        (Cause::Driver, line_with("GLFW error").or_else(|| line_with("OpenGL")))
    } else {
        (Cause::Unknown, None)
    };
    Crash { cause, detail, report: report_path, auto: false }
}

fn mixin_mod(text: &str) -> Option<String> {
    ["from mod ", "for mod "].iter().find_map(|m| {
        let rest = &text[text.find(m)? + m.len()..];
        let id: String = rest.chars().take_while(|c| c.is_ascii_alphanumeric() || "_-.".contains(*c)).collect();
        (!id.is_empty()).then_some(id)
    })
}

pub fn report_path(log: &str) -> Option<String> {
    log.lines().rev().find_map(|l| Some(l[l.find(REPORT_MARK)? + REPORT_MARK.len()..].trim().to_string())).filter(|p| !p.is_empty())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn cause(log: &str) -> (Cause, Option<String>) {
        let c = analyze(log, None, "");
        (c.cause, c.detail)
    }

    #[test]
    fn names_common_causes() {
        assert_eq!(cause("Exception in thread \"main\" java.lang.OutOfMemoryError: Java heap space").0, Cause::OutOfMemory);
        assert_eq!(cause("Error occurred during initialization of VM\nCould not reserve enough space for 8388608KB object heap").0, Cause::TooMuchMemory);
        assert_eq!(
            cause("java.lang.UnsupportedClassVersionError: net/x/Main has been compiled by a more recent version of the Java Runtime (class file version 65.0)"),
            (Cause::WrongJava, Some("java.lang.UnsupportedClassVersionError: net/x/Main has been compiled by a more recent version of the Java Runtime (class file version 65.0)".into()))
        );
        assert_eq!(
            cause("Incompatible mods found!\n\t - Mod 'Sodium Extra' (sodium-extra) 0.5 requires any version of mod 'Sodium' (sodium), which is missing!"),
            (Cause::MissingMod, Some("Mod 'Sodium Extra' (sodium-extra) 0.5 requires any version of mod 'Sodium' (sodium), which is missing!".into()))
        );
        assert_eq!(cause("net.fabricmc.loader.impl.discovery.ModResolutionException: Found duplicate mods").0, Cause::DuplicateMod);
        assert_eq!(
            cause("org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError: An unexpected critical error was encountered\nMixin [sodium.mixins.json:core.MixinWindow] from mod sodium failed injection check"),
            (Cause::BrokenMod, Some("sodium".into()))
        );
        assert_eq!(cause("[LWJGL] GLFW error 65542: WGL: The driver does not appear to support OpenGL").0, Cause::Driver);
        assert_eq!(cause("Something odd").0, Cause::Unknown);
    }

    #[test]
    fn reads_the_crash_report_too() {
        assert_eq!(analyze("Game crashed", None, "Description: Out of memory\njava.lang.OutOfMemoryError").cause, Cause::OutOfMemory);
    }

    #[test]
    fn finds_report_path() {
        let log = "[12:00] [Render thread/ERROR]: #@!@# Game crashed! Crash report saved to: #@!@# /g/crash-reports/crash-1.txt";
        assert_eq!(report_path(log).as_deref(), Some("/g/crash-reports/crash-1.txt"));
        assert_eq!(report_path("nothing"), None);
    }
}
