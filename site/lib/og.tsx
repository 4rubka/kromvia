import { ImageResponse } from "next/og";

export const OG_SIZE = { width: 1200, height: 630 };

const BG = "radial-gradient(ellipse 70% 60% at 70% 0%, rgba(31,99,214,0.35), transparent 70%), linear-gradient(135deg, #0b0a14, #06050b)";

export function ogCard({ art, title, lines, badge }: { art?: string; title: string; lines: string[]; badge?: string }) {
  return new ImageResponse(
    (
      <div style={{ width: "100%", height: "100%", display: "flex", background: BG, color: "#f3f1fa", padding: 64, fontFamily: "sans-serif" }}>
        <div style={{ display: "flex", flexDirection: "column", justifyContent: "space-between", flex: 1 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 16, fontSize: 34, fontWeight: 700 }}>
            <img src="https://kromvia.com/logo-mark.svg" width={52} height={52} alt="" />
            Kromvia
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: 18 }}>
            {badge && (
              <div style={{ display: "flex", alignSelf: "flex-start", padding: "6px 16px", borderRadius: 999, background: "linear-gradient(120deg, #a35fd6, #6a5cf0 45%, #1f63d6)", fontSize: 24 }}>
                {badge}
              </div>
            )}
            <div style={{ display: "flex", fontSize: title.length > 18 ? 64 : 84, fontWeight: 800, lineHeight: 1.05 }}>{title}</div>
            {lines.map((l) => (
              <div key={l} style={{ display: "flex", fontSize: 32, color: "#c3bdd6" }}>
                {l}
              </div>
            ))}
          </div>
        </div>
        {art && (
          <div style={{ display: "flex", alignItems: "center", justifyContent: "center", width: 380 }}>
            <img src={art} height={460} alt="" style={{ objectFit: "contain" }} />
          </div>
        )}
      </div>
    ),
    OG_SIZE,
  );
}
