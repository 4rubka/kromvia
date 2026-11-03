import type { Metadata } from "next";
import { Glyph } from "@/components/Glyph";
import { release } from "@/lib/api";
import s from "./download.module.css";

export const metadata: Metadata = { title: "Download", description: "Download the Kromvia launcher for Windows, macOS and Linux." };

const PLATFORMS = [
  { name: "Windows", note: "Windows 10 and 11, 64-bit", bee: "os-windows", files: [{ id: "windows", label: "Download" }] },
  { name: "macOS", note: "Apple Silicon", bee: "os-macos", files: [{ id: "macos", label: "Download" }] },
  {
    name: "Linux",
    note: "AppImage for any distro, or a .deb for Debian and Ubuntu",
    bee: "os-linux",
    files: [
      { id: "linux-appimage", label: "AppImage" },
      { id: "linux-deb", label: ".deb" },
    ],
  },
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
          const files = p.files.flatMap((f) => {
            const file = rel?.files.find((r) => r.platform === f.id);
            return file ? [{ ...file, label: f.label }] : [];
          });
          return (
            <div key={p.name} className={`block ${s.card}`}>
              <img src={`/bee/${p.bee}.webp`} alt="" className={s.bee} />
              <h2>{p.name}</h2>
              <p className={`dim ${s.note}`}>{p.note}</p>
              <div className={s.buttons}>
                {files.length ? (
                  files.map((f) => (
                    <a key={f.platform} href={f.url} className="btn">
                      <Glyph name="download" size={16} /> {f.label} <span className={s.size}>{mb(f.size)}</span>
                    </a>
                  ))
                ) : (
                  <span className="btn ghost" aria-disabled="true">
                    Coming soon
                  </span>
                )}
              </div>
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
