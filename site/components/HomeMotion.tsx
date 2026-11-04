"use client";

import gsap from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import { SplitText } from "gsap/SplitText";
import { ScrambleTextPlugin } from "gsap/ScrambleTextPlugin";
import { useGSAP } from "@gsap/react";

gsap.registerPlugin(ScrollTrigger, SplitText, ScrambleTextPlugin, useGSAP);

const all = (sel: string) => Array.from(document.querySelectorAll<HTMLElement>(sel));

function hero() {
  const title = document.querySelector("[data-m=hero-title]");
  const lede = document.querySelector("[data-m=hero-lede]");
  gsap.set("[data-hide]", { visibility: "visible" });

  SplitText.create(title, {
    type: "words,chars",
    mask: "words",
    autoSplit: true,
    onSplit: (self) =>
      gsap.from(self.chars, { yPercent: 120, rotate: 14, opacity: 0, duration: 0.9, ease: "back.out(2)", stagger: 0.018, delay: 0.1 }),
  });

  SplitText.create(lede, {
    type: "lines",
    mask: "lines",
    autoSplit: true,
    onSplit: (self) => gsap.from(self.lines, { yPercent: 105, duration: 0.9, ease: "power4.out", stagger: 0.08, delay: 0.45 }),
  });

  gsap.from("[data-m=hero-actions] > *", { y: 40, opacity: 0, scale: 0.85, duration: 0.8, ease: "back.out(2.2)", stagger: 0.1, delay: 0.75 });

  gsap.from("[data-m=hero-mock]", {
    y: 90,
    rotationX: 28,
    rotationY: -18,
    scale: 0.82,
    opacity: 0,
    transformPerspective: 1400,
    duration: 1.6,
    ease: "expo.out",
    delay: 0.25,
  });

  gsap.to("[data-m=hero-text]", {
    yPercent: -35,
    opacity: 0,
    ease: "none",
    scrollTrigger: { trigger: "[data-m=hero]", start: "top top", end: "bottom top", scrub: true },
  });
  gsap.to("[data-m=hero-mock]", {
    yPercent: -12,
    ease: "none",
    scrollTrigger: { trigger: "[data-m=hero]", start: "top top", end: "bottom top", scrub: true },
  });

  const tilt = document.querySelector("[data-m=hero-tilt]");
  gsap.set(tilt, { transformPerspective: 1400 });
  const rx = gsap.quickTo(tilt, "rotationX", { duration: 0.8, ease: "power3.out" });
  const ry = gsap.quickTo(tilt, "rotationY", { duration: 0.8, ease: "power3.out" });
  const heroEl = document.querySelector<HTMLElement>("[data-m=hero]")!;
  const move = (e: PointerEvent) => {
    const r = heroEl.getBoundingClientRect();
    rx(((e.clientY - r.top) / r.height - 0.5) * -6);
    ry(((e.clientX - r.left) / r.width - 0.5) * 9);
  };
  const leave = () => (rx(0), ry(0));
  heroEl.addEventListener("pointermove", move);
  heroEl.addEventListener("pointerleave", leave);
  return () => {
    heroEl.removeEventListener("pointermove", move);
    heroEl.removeEventListener("pointerleave", leave);
  };
}

function cubes() {
  const layers = all("[data-cubes]");
  layers.forEach((layer) => {
    gsap.from(layer.children, { opacity: 0, duration: 1.2, ease: "power2.out", stagger: { each: 0.08, from: "random" }, delay: 0.2 });
    gsap.fromTo(
      layer,
      { "--sy": "120px" },
      { "--sy": "-120px", ease: "none", scrollTrigger: { trigger: layer.parentElement, start: "top bottom", end: "bottom top", scrub: 0.6 } },
    );
  });
  const move = (e: PointerEvent) => {
    gsap.to(layers, {
      "--mx": `${(e.clientX / innerWidth - 0.5) * -50}px`,
      "--my": `${(e.clientY / innerHeight - 0.5) * -50}px`,
      duration: 1.2,
      ease: "power3.out",
      overwrite: "auto",
    });
  };
  addEventListener("pointermove", move);
  return () => removeEventListener("pointermove", move);
}

function headings() {
  all("main .section-head h2").forEach((h) => {
    SplitText.create(h, {
      type: "words,chars",
      mask: "words",
      autoSplit: true,
      onSplit: (self) =>
        gsap.from(self.chars, {
          yPercent: 110,
          rotate: 10,
          duration: 0.9,
          ease: "power4.out",
          stagger: 0.016,
          scrollTrigger: { trigger: h, start: "top 85%" },
        }),
    });
  });

  all("main .section-head > p, [data-m=social] p").forEach((p) => {
    SplitText.create(p, {
      type: "lines",
      mask: "lines",
      autoSplit: true,
      onSplit: (self) =>
        gsap.from(self.lines, { yPercent: 105, duration: 0.8, ease: "power3.out", stagger: 0.07, delay: 0.15, scrollTrigger: { trigger: p, start: "top 88%" } }),
    });
  });

  all("[data-m=social] b").forEach((b) => {
    gsap.from(b, { x: -40, opacity: 0, duration: 0.7, ease: "power3.out", scrollTrigger: { trigger: b, start: "top 88%" } });
  });
}

function cards() {
  const list = all("[data-tone]");
  gsap.set(list, { opacity: 0, y: 110, scale: 0.9, rotation: () => gsap.utils.random(-6, 6), transformPerspective: 1200 });
  ScrollTrigger.batch(list, {
    start: "top 90%",
    once: true,
    onEnter: (batch) => {
      gsap.to(batch, { opacity: 1, y: 0, scale: 1, rotation: 0, duration: 1, ease: "back.out(1.4)", stagger: 0.12 });
      const bees = batch.map((c) => c.querySelector("img")).filter(Boolean);
      gsap.fromTo(
        bees,
        { scale: 0, rotation: -35, transformOrigin: "80% 100%", transition: "none" },
        { scale: 1, rotation: 0, duration: 1.3, ease: "elastic.out(1, 0.5)", stagger: 0.12, delay: 0.3, clearProps: "transform,transition" },
      );
    },
  });

  const cleanups = list.map((card) => {
    const rx = gsap.quickTo(card, "rotationX", { duration: 0.5, ease: "power3.out" });
    const ry = gsap.quickTo(card, "rotationY", { duration: 0.5, ease: "power3.out" });
    const move = (e: PointerEvent) => {
      const r = card.getBoundingClientRect();
      rx(((e.clientY - r.top) / r.height - 0.5) * -10);
      ry(((e.clientX - r.left) / r.width - 0.5) * 12);
    };
    const leave = () => (rx(0), ry(0));
    card.addEventListener("pointermove", move);
    card.addEventListener("pointerleave", leave);
    return () => {
      card.removeEventListener("pointermove", move);
      card.removeEventListener("pointerleave", leave);
    };
  });
  return () => cleanups.forEach((c) => c());
}

function marquee() {
  const el = document.querySelector<HTMLElement>("[data-m=marquee]");
  if (!el) return;
  gsap.from(el, { opacity: 0, scale: 0.92, duration: 1, ease: "power3.out", scrollTrigger: { trigger: el, start: "top 92%" } });
  let target = 0;
  let cur = 0;
  ScrollTrigger.create({
    trigger: el,
    start: "top bottom",
    end: "bottom top",
    onUpdate: (self) => (target = gsap.utils.clamp(-18, 18, self.getVelocity() / -120)),
  });
  const tick = () => {
    target *= 0.9;
    cur += (target - cur) * 0.12;
    gsap.set(el, { skewX: cur });
  };
  gsap.ticker.add(tick);
  return () => gsap.ticker.remove(tick);
}

function sections() {
  const cosm = document.querySelector("[data-m=cosm] > :first-child");
  if (cosm)
    gsap.from(cosm, {
      x: -120,
      rotationY: 25,
      opacity: 0,
      transformPerspective: 1200,
      duration: 1.3,
      ease: "expo.out",
      scrollTrigger: { trigger: cosm, start: "top 80%" },
    });

  const btns = all("[data-m=cosm] .btn");
  gsap.from(btns, { y: 30, opacity: 0, duration: 0.7, ease: "back.out(2)", scrollTrigger: { trigger: btns[0], start: "top 92%" } });

  const rooms = document.querySelector("[data-m=rooms]");
  if (rooms)
    gsap.fromTo(
      rooms,
      { yPercent: 35, rotation: -18, scale: 0.6, opacity: 0 },
      { yPercent: 0, rotation: 0, scale: 1, opacity: 1, ease: "power2.out", scrollTrigger: { trigger: rooms, start: "top bottom", end: "center 55%", scrub: 0.8 } },
    );

  const final = document.querySelector<HTMLElement>("[data-m=final]");
  if (!final) return;
  const tl = gsap.timeline({ scrollTrigger: { trigger: final, start: "top 70%" } });
  const h = final.querySelector("h2")!;
  tl.from(final.querySelector("img"), { scale: 0, rotation: 200, duration: 1.4, ease: "elastic.out(1, 0.45)" })
    .to(h, { duration: 1.6, scrambleText: { text: h.textContent ?? "", chars: "KROMVIA▚▞▙▟", speed: 0.5, revealDelay: 0.4 } }, 0.15)
    .from(final.querySelectorAll("p, .btn"), { y: 40, opacity: 0, duration: 0.8, ease: "back.out(1.8)", stagger: 0.1 }, 0.5);
}

function magnetic() {
  const cleanups = all("main .btn").map((btn) => {
    const x = gsap.quickTo(btn, "x", { duration: 0.4, ease: "power3.out" });
    const y = gsap.quickTo(btn, "y", { duration: 0.4, ease: "power3.out" });
    const move = (e: PointerEvent) => {
      const r = btn.getBoundingClientRect();
      x((e.clientX - r.left - r.width / 2) * 0.3);
      y((e.clientY - r.top - r.height / 2) * 0.4);
    };
    const leave = () => gsap.to(btn, { x: 0, y: 0, duration: 0.9, ease: "elastic.out(1, 0.35)", overwrite: true });
    btn.addEventListener("pointermove", move);
    btn.addEventListener("pointerleave", leave);
    return () => {
      btn.removeEventListener("pointermove", move);
      btn.removeEventListener("pointerleave", leave);
    };
  });
  return () => cleanups.forEach((c) => c());
}

export function HomeMotion() {
  useGSAP(() => {
    const mm = gsap.matchMedia();
    mm.add("(prefers-reduced-motion: no-preference)", () => {
      const fine = matchMedia("(pointer: fine)").matches;
      const off = [hero(), cubes(), headings(), cards(), marquee(), sections(), fine ? magnetic() : undefined];
      return () => off.forEach((f) => f?.());
    });
  });
  return null;
}
