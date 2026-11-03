import Link from "next/link";

export default function NotFound() {
  return (
    <div className="wrap" style={{ display: "grid", justifyItems: "center", gap: 16, padding: "100px 0 120px", textAlign: "center" }}>
      <img src="/bee/not-found.webp" alt="" width={154} height={149} />
      <h1 style={{ fontSize: "clamp(32px, 5vw, 52px)" }}>Nothing here</h1>
      <p className="muted">The bee looked everywhere. This page doesn&apos;t exist, or it moved.</p>
      <Link href="/" className="btn" style={{ marginTop: 8 }}>
        Back home
      </Link>
    </div>
  );
}
