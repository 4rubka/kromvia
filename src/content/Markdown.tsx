import { Fragment, type ReactNode } from "react";
import { openUrl } from "@tauri-apps/plugin-opener";

function inline(text: string): ReactNode[] {
  const out: ReactNode[] = [];
  const re = /!\[[^\]]*\]\([^)]*\)|\[([^\]]+)\]\((https?:\/\/[^)\s]+)[^)]*\)|\*\*([^*]+)\*\*|__([^_]+)__|\*([^*]+)\*|`([^`]+)`|<[^>]+>/g;
  let last = 0;
  for (const m of text.matchAll(re)) {
    if (m.index > last) out.push(text.slice(last, m.index));
    const [, linkText, href, bold, bold2, italic, code] = m;
    const key = out.length;
    if (href) {
      out.push(
        <a key={key} href={href} onClick={(e) => (e.preventDefault(), openUrl(href))}>
          {linkText}
        </a>,
      );
    } else if (bold || bold2) out.push(<strong key={key}>{bold ?? bold2}</strong>);
    else if (italic) out.push(<em key={key}>{italic}</em>);
    else if (code) out.push(<code key={key}>{code}</code>);
    last = m.index + m[0].length;
  }
  if (last < text.length) out.push(text.slice(last));
  return out;
}

/** A small, safe Markdown renderer for Modrinth project pages: headings, lists, quotes, code blocks and paragraphs. */
export function Markdown({ source }: { source: string }) {
  const blocks: ReactNode[] = [];
  const lines = source.replace(/\r/g, "").split("\n");
  let para: string[] = [];
  let list: string[] = [];
  const flush = () => {
    if (para.length) blocks.push(<p key={blocks.length}>{inline(para.join(" "))}</p>);
    if (list.length)
      blocks.push(
        <ul key={blocks.length}>
          {list.map((item, i) => (
            <li key={i}>{inline(item)}</li>
          ))}
        </ul>,
      );
    para = [];
    list = [];
  };
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (line.trim().startsWith("```")) {
      flush();
      const code: string[] = [];
      while (++i < lines.length && !lines[i].trim().startsWith("```")) code.push(lines[i]);
      blocks.push(<pre key={blocks.length}>{code.join("\n")}</pre>);
      continue;
    }
    const heading = line.match(/^(#{1,6})\s+(.*)/);
    const item = line.match(/^\s*(?:[-*+]|\d+\.)\s+(.*)/);
    if (heading) {
      flush();
      const level = Math.min(heading[1].length + 2, 6);
      const Tag = `h${level}` as "h3";
      blocks.push(<Tag key={blocks.length}>{inline(heading[2])}</Tag>);
    } else if (item) {
      if (para.length) flush();
      list.push(item[1]);
    } else if (line.startsWith(">")) {
      flush();
      blocks.push(<blockquote key={blocks.length}>{inline(line.replace(/^>\s?/, ""))}</blockquote>);
    } else if (!line.trim() || /^(-{3,}|\*{3,})$/.test(line.trim())) {
      flush();
    } else {
      if (list.length) flush();
      para.push(line.trim());
    }
  }
  flush();
  return <div className="markdown">{blocks.map((b, i) => <Fragment key={i}>{b}</Fragment>)}</div>;
}
