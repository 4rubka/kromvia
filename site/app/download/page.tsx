import type { Metadata } from "next";
import { Glyph } from "@/components/Glyph";
import { release } from "@/lib/api";
import s from "./download.module.css";

export const metadata: Metadata = { title: "Download", description: "Download the Kromvia launcher for Windows, macOS and Linux." };

const PLATFORMS = [
  { id: "windows", name: "Windows", note: "Windows 10 and 11, 64-bit" },
  { id: "macos", name: "macOS", note: "Apple Silicon" },
  { id: "linux-appimage", name: "Linux", note: "AppImage, any distro" },
  { id: "linux-deb", name: "Debian / Ubuntu", note: ".deb package" },
];

const mb = (n: number) => `${(n / 1024 / 1024).toFixed(0)} MB`;

export default async function Download() {
  const rel = await release();
  return (
    <div className="wrap">
      <header className={`page-head ${s.head}`}>
        <h1>Get Kromvia</h1>
        {rel ? (
          <p>
            Version {rel.version}, released {new Date(rel.published).toLocaleDateString("en", { day: "numeric", month: "long", year: "numeric" })}. It updates itself after that.
          </p>
        ) : (
          <p>The first release is on its way. Check back soon.</p>
        )}
      </header>
      <div className={s.grid}>
        {PLATFORMS.map((p) => {
          const file = rel?.files.find((f) => f.platform === p.id);
          return (
            <div key={p.id} className={`block ${s.card}`}>
              <h2>{p.name}</h2>
              <p className="dim">{p.note}</p>
              {file ? (
                <a href={file.url} className="btn">
                  <Glyph name="download" size={16} /> Download <span className={s.size}>{mb(file.size)}</span>
                </a>
              ) : (
                <span className="btn ghost" aria-disabled="true">
                  Coming soon
                </span>
              )}
            </div>
          );
        })}
      </div>
      <div className={s.after}>
        <img src={rel ? "/bee/success.webp" : "/bee/loading.webp"} alt="" />
        <div>
          <h3>After installing</h3>
          <ol>
            <li>Sign in with Microsoft, or play offline with a nickname.</li>
            <li>Create an instance: pick a version and a loader.</li>
            <li>Press Play. In game, Right Shift opens the Kromvia menu.</li>
          </ol>
        </div>
      </div>
    </div>
  );
}
