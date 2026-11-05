
use serde::Serialize;

#[derive(Serialize, Clone, Debug, PartialEq)]
pub struct LogLine {
    pub level: String,
    pub thread: String,
    pub text: String,
    pub noise: bool,
    #[serde(skip)]
    pub logger: String,
}

#[derive(Default)]
pub struct LogParser {
    event: Option<String>,
}

impl LogParser {
    pub fn push(&mut self, line: &str) -> Option<LogLine> {
        if let Some(buf) = &mut self.event {
            buf.push_str(line);
            buf.push('\n');
            if !line.contains("</log4j:Event>") {
                return None;
            }
            let buf = self.event.take().unwrap();
            return Some(parse_event(&buf));
        }
        if line.trim_start().starts_with("<log4j:Event") {
            if line.contains("</log4j:Event>") {
                return Some(parse_event(line));
            }
            self.event = Some(format!("{line}\n"));
            return None;
        }
        Some(plain(line))
    }
}

fn parse_event(xml: &str) -> LogLine {
    let mut text = cdata(xml, "log4j:Message").unwrap_or_default();
    if let Some(trace) = cdata(xml, "log4j:Throwable") {
        text.push('\n');
        text.push_str(trace.trim_end());
    }
    let level = attr(xml, "level").unwrap_or_else(|| "INFO".into());
    let noise = is_noise(&text);
    LogLine { level, thread: attr(xml, "thread").unwrap_or_default(), text, noise, logger: attr(xml, "logger").unwrap_or_default() }
}

/// Forge and NeoForge swap in their own console pattern once FML is up:
/// `[12:00:00] [Render thread/INFO] [kromvia-presence/]: text`.
fn fml(line: &str) -> Option<LogLine> {
    let rest = line.strip_prefix('[')?;
    let rest = &rest[rest.find("] [")? + 3..];
    let (thread_level, rest) = rest.split_once("] [")?;
    let (logger_marker, text) = rest.split_once("]: ")?;
    let (thread, level) = thread_level.rsplit_once('/')?;
    let logger = logger_marker.split_once('/').map_or(logger_marker, |(l, _)| l);
    Some(LogLine { level: level.into(), thread: thread.into(), text: text.into(), noise: is_noise(text), logger: logger.into() })
}

fn plain(line: &str) -> LogLine {
    if let Some(l) = fml(line) {
        return l;
    }
    let level = ["FATAL", "ERROR", "WARN", "DEBUG"]
        .into_iter()
        .find(|l| line.contains(&format!("/{l}]")))
        .unwrap_or("INFO");
    LogLine { level: level.into(), thread: String::new(), text: line.to_string(), noise: is_noise(line), logger: String::new() }
}

fn attr(xml: &str, name: &str) -> Option<String> {
    let start = xml.find(&format!(" {name}=\""))? + name.len() + 3;
    let end = start + xml[start..].find('"')?;
    Some(unescape(&xml[start..end]))
}

fn cdata(xml: &str, tag: &str) -> Option<String> {
    let open = format!("<{tag}><![CDATA[");
    let start = xml.find(&open)? + open.len();
    let end = start + xml[start..].find(&format!("]]></{tag}>"))?;
    Some(xml[start..end].to_string())
}

fn unescape(s: &str) -> String {
    s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
}

const NOISE: &[&str] = &[
    "Received passengers for unknown entity",
    "Ignoring unknown attribute",
    "Missing sound for event",
    "Unable to load font",
    "Couldn't find data",
    "Shader rendertype_",
    "Narrator library",
    "ALSA lib ",
    "Unknown custom packet identifier",
    "Reloading ResourceManager",
    "OpenAL initialized",
    "Sound engine started",
    "Created: ",
    "Found non-pack entry",
];

fn is_noise(text: &str) -> bool {
    NOISE.iter().any(|n| text.contains(n))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_xml_events() {
        let mut p = LogParser::default();
        assert_eq!(p.push(r#"<log4j:Event logger="a" timestamp="1" level="WARN" thread="Render thread">"#), None);
        assert_eq!(p.push("  <log4j:Message><![CDATA[Missing sound for event: x]]></log4j:Message>"), None);
        let line = p.push("</log4j:Event>").unwrap();
        assert_eq!(line.level, "WARN");
        assert_eq!(line.thread, "Render thread");
        assert_eq!(line.logger, "a");
        assert_eq!(line.text, "Missing sound for event: x");
        assert!(line.noise);
    }

    #[test]
    fn keeps_multiline_messages_and_traces() {
        let mut p = LogParser::default();
        p.push(r#"<log4j:Event logger="a" timestamp="1" level="ERROR" thread="main">"#);
        p.push("  <log4j:Message><![CDATA[first");
        p.push("second]]></log4j:Message>");
        p.push("  <log4j:Throwable><![CDATA[java.lang.Exception: boom");
        p.push("\tat x.y(Z.java:1)");
        p.push("]]></log4j:Throwable>");
        let line = p.push("</log4j:Event>").unwrap();
        assert_eq!(line.text, "first\nsecond\njava.lang.Exception: boom\n\tat x.y(Z.java:1)");
        assert!(!line.noise);
    }

    #[test]
    fn parses_plain_lines() {
        let mut p = LogParser::default();
        let line = p.push("[12:00:00] [main/WARN]: Mixin something").unwrap();
        assert_eq!(line.level, "WARN");
        assert_eq!(p.push("hello").unwrap().level, "INFO");
    }

    #[test]
    fn parses_fml_lines() {
        let mut p = LogParser::default();
        let line = p.push(r#"[08:47:31] [Render thread/INFO] [kromvia-presence/]: {"social":{"do":"load"}}"#).unwrap();
        assert_eq!(line.logger, "kromvia-presence");
        assert_eq!(line.thread, "Render thread");
        assert_eq!(line.level, "INFO");
        assert_eq!(line.text, r#"{"social":{"do":"load"}}"#);
        let line = p.push("[08:47:31] [main/WARN] [ne.ne.fm.lo.Thing/LOADING]: odd ]: text").unwrap();
        assert_eq!((line.level.as_str(), line.logger.as_str(), line.text.as_str()), ("WARN", "ne.ne.fm.lo.Thing", "odd ]: text"));
    }
}
