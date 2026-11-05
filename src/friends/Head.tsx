import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";

type Found = { dataUrl: string; slim: boolean };

const KEY = "kromvia.heads";
const DAY = 86_400_000;
const skins = new Map<string, Promise<Found | null>>();
const heads = new Map<string, Promise<string | null>>();
let queue = Promise.resolve();

function stored(): Record<string, { at: number; head: string | null }> {
  try {
    return JSON.parse(localStorage.getItem(KEY) ?? "{}");
  } catch {
    return {};
  }
}

export function skinOf(name: string): Promise<Found | null> {
  const key = name.toLowerCase();
  let p = skins.get(key);
  if (!p) {
    const run = queue.then(() => invoke<Found>("skin_lookup", { nick: name }).catch(() => (key === "mhf_steve" ? null : skinOf("MHF_Steve"))));
    queue = run.then(() => undefined);
    p = run;
    skins.set(key, p);
  }
  return p;
}

function crop(src: string): Promise<string | null> {
  return new Promise((resolve) => {
    const img = new Image();
    img.onload = () => {
      const c = document.createElement("canvas");
      c.width = c.height = 8;
      const g = c.getContext("2d")!;
      g.drawImage(img, 8, 8, 8, 8, 0, 0, 8, 8);
      g.drawImage(img, 40, 8, 8, 8, 0, 0, 8, 8);
      resolve(c.toDataURL("image/png"));
    };
    img.onerror = () => resolve(null);
    img.src = src;
  });
}

export function headOf(name: string): Promise<string | null> {
  const key = name.toLowerCase();
  let p = heads.get(key);
  if (!p) {
    const hit = stored()[key];
    p =
      hit && Date.now() - hit.at < DAY
        ? Promise.resolve(hit.head)
        : skinOf(name)
            .then((s) => (s ? crop(s.dataUrl) : null))
            .then((head) => {
              try {
                localStorage.setItem(KEY, JSON.stringify({ ...stored(), [key]: { at: Date.now(), head } }));
              } catch {}
              return head;
            });
    heads.set(key, p);
  }
  return p;
}

export function Head({ name, size = 24 }: { name: string; size?: number }) {
  const [src, setSrc] = useState<string | null>(null);
  useEffect(() => {
    let live = true;
    setSrc(null);
    headOf(name).then((h) => live && setSrc(h));
    return () => {
      live = false;
    };
  }, [name]);
  return src ? (
    <img className="head" src={src} width={size} height={size} alt="" />
  ) : (
    <span className="head" style={{ width: size, height: size }} />
  );
}
