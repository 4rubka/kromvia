#!/bin/sh
set -e
cd "$(dirname "$0")/.."
mkdir -p public
cp -r ../public/bee ../public/models ../public/logo.svg ../public/hero.webm ../public/hero.mp4 public/
cp art/bee/*.webp public/bee/
cp ../design/logo-mark.svg public/logo-mark.svg
mkdir -p launcher/art launcher/ui
cp ../src/art/capes.ts ../src/art/procedural.ts ../src/art/cosmetics.json ../src/art/wear.ts ../src/art/gltf.ts ../src/art/catalog.ts launcher/art/
cp ../src/ui/Icon.tsx launcher/ui/
cp ../design/1793298752922-01a188ee-e648-75aa-9ad1-0e5bf196abd2.png public/banner.png
