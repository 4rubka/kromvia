import type { Metadata, Viewport } from "next";
import { Manrope, Unbounded } from "next/font/google";
import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";
import "./globals.css";

const sans = Manrope({ variable: "--font-sans", subsets: ["latin", "cyrillic"] });
const display = Unbounded({ variable: "--font-display", subsets: ["latin", "cyrillic"], weight: ["500", "600", "700"] });

export const metadata: Metadata = {
  metadataBase: new URL("https://kromvia.com"),
  title: { default: "Kromvia: Minecraft launcher with a built-in client", template: "%s · Kromvia" },
  description: "A Minecraft launcher for any version and loader, with a built-in PvP and HUD client, cosmetics, Rooms and friends.",
  icons: { icon: "/logo.svg" },
  openGraph: { siteName: "Kromvia", images: ["/banner.png"], type: "website" },
  twitter: { card: "summary_large_image", images: ["/banner.png"] },
};

export const viewport: Viewport = { themeColor: "#06050b", colorScheme: "dark" };

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className={`${sans.variable} ${display.variable}`}>
      <body>
        <Header />
        <main>{children}</main>
        <Footer />
      </body>
    </html>
  );
}
