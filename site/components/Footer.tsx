import Link from "next/link";
import s from "./Footer.module.css";

export function Footer() {
  return (
    <footer className={s.footer}>
      <div className={s.lineup} aria-hidden="true">
        <img src="/bee/welcome.webp" alt="" className={s.b1} />
        <img src="/bee/friends-online.webp" alt="" className={s.b2} />
        <img src="/bee/empty-home.webp" alt="" className={s.b3} />
        <img src="/bee/success.webp" alt="" className={s.b4} />
      </div>
      <div className={s.base}>
      <div className={`wrap ${s.grid}`}>
        <Link href="/" className={s.brand} aria-label="Kromvia home">
          <img src="/logo-mark.svg" alt="" width={36} height={36} />
          <div>
            <strong>Kromvia</strong>
            <p className="dim">Play · Create · Together</p>
          </div>
        </Link>
        <nav className={s.col}>
          <b>Kromvia</b>
          <Link href="/download">Download</Link>
          <Link href="/features">Features</Link>
          <Link href="/cosmetics">Cosmetics</Link>
          <Link href="/changelog">Changelog</Link>
        </nav>
        <nav className={s.col}>
          <b>Community</b>
          <Link href="/top">Top players</Link>
          <Link href="/rooms">Rooms</Link>
          <Link href="/faq">FAQ</Link>
        </nav>
        <nav className={s.col}>
          <b>Legal</b>
          <Link href="/privacy">Privacy</Link>
          <Link href="/terms">Terms</Link>
        </nav>
      </div>
      <div className={`wrap ${s.legal}`}>
        <span>© {new Date().getFullYear()} Kromvia</span>
        <span>Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.</span>
      </div>
      </div>
    </footer>
  );
}
