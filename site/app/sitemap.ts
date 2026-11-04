import type { MetadataRoute } from "next";

const PAGES = ["", "download", "features", "cosmetics", "rooms", "top", "faq", "changelog", "privacy", "terms"];

export default function sitemap(): MetadataRoute.Sitemap {
  return PAGES.map((p) => ({ url: `https://kromvia.com/${p}`, priority: p === "" ? 1 : p === "download" ? 0.9 : 0.5 }));
}
