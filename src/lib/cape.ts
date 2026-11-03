import { pushOutfit, savedOutfit, storeOutfit } from "./outfit";

export function capeOn() {
  return savedOutfit().back === "cape";
}

export function setCapeOn(on: boolean) {
  const o = savedOutfit();
  const next = { ...o, back: on ? "cape" : o.back === "cape" ? "none" : o.back } as const;
  storeOutfit(next);
  pushOutfit(next).catch(() => {});
}
