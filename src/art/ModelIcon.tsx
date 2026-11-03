import { useMemo } from "react";
import type { Instance, LoaderKind } from "../lib/launcher";
import type { BlockKind } from "./blocks";
import { snapshot } from "./snapshot";

const BY_LOADER: Record<LoaderKind | "vanilla", BlockKind> = {
  fabric: "grass",
  legacyfabric: "grass",
  ornithe: "grass",
  quilt: "wool",
  forge: "crafting",
  neoforge: "crafting",
  vanilla: "stone",
};

export const ICON_BLOCKS: BlockKind[] = ["grass", "crafting", "wool", "stone", "earth", "portal"];

export function BlockIcon({ kind, size = 40 }: { kind: BlockKind; size?: number }) {
  const src = useMemo(() => {
    try {
      return snapshot(kind);
    } catch {
      return null;
    }
  }, [kind]);
  return src ? <img className="model-icon" src={src} width={size} height={size} alt="" /> : <span className="model-icon" style={{ width: size, height: size }} />;
}

export function ModelIcon({ loader, size = 40 }: { loader: LoaderKind | "vanilla"; size?: number }) {
  return <BlockIcon kind={BY_LOADER[loader]} size={size} />;
}

export function InstanceIcon({ inst, size = 40 }: { inst: Instance; size?: number }) {
  const icon = inst.icon;
  if (icon?.startsWith("data:image/png")) return <img className="model-icon custom" src={icon} width={size} height={size} alt="" />;
  const block = icon?.startsWith("block:") ? (icon.slice("block:".length) as BlockKind) : null;
  if (block && ICON_BLOCKS.includes(block)) return <BlockIcon kind={block} size={size} />;
  return <ModelIcon loader={inst.loader?.kind ?? "vanilla"} size={size} />;
}
