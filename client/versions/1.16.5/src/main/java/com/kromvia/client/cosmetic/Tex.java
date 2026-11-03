package com.kromvia.client.cosmetic;

import java.util.Random;

import com.mojang.blaze3d.platform.NativeImage;

final class Tex {
	enum Face { TOP, BOTTOM, WEST, NORTH, EAST, SOUTH }

	private Tex() {}

	private static int base(Face f) {
		switch (f) {
			case TOP: return 238;
			case NORTH: return 218;
			case SOUTH: return 206;
			case WEST: case EAST: return 198;
			default: return 170;
		}
	}

	private static float hash(int seed, int x, int y) {
		int h = seed ^ x * 374761393 ^ y * 668265263;
		h = (h ^ h >>> 13) * 1274126177;
		return ((h ^ h >>> 16) & 0xFFFF) / 32767.5f - 1;
	}

	private static float noise(int seed, float x, float y, float cell) {
		x /= cell;
		y /= cell;
		int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
		float fx = x - xi, fy = y - yi;
		fx = fx * fx * (3 - 2 * fx);
		fy = fy * fy * (3 - 2 * fy);
		float a = hash(seed, xi, yi), b = hash(seed, xi + 1, yi);
		float c = hash(seed, xi, yi + 1), d = hash(seed, xi + 1, yi + 1);
		return a + (b - a) * fx + (c - a + (d - c - b + a) * fx) * fy;
	}

	private static float smooth(float e0, float e1, float v) {
		float t = Math.max(0, Math.min(1, (v - e0) / (e1 - e0)));
		return t * t * (3 - 2 * t);
	}

	static void face(NativeImage img, Pattern p, Face f, int rx, int ry, int rw, int rh, Random rnd) {
		if (rw <= 0 || rh <= 0) return;
		int col0 = rnd.nextInt(97);
		int seed = rnd.nextInt();
		boolean side = f != Face.TOP && f != Face.BOTTOM;
		int thin = Math.min(rw, rh);
		for (int y = 0; y < rh; y++) {
			for (int x = 0; x < rw; x++) {
				int px = rx + x, py = ry + y;
				if (px >= img.getWidth() || py >= img.getHeight()) continue;
				float lum = base(f) + noise(seed, x, y, 7) * 9 + noise(seed + 1, x, y, 2.5f) * 4;
				if (side) lum += 8 - 16f * y / Math.max(rh - 1, 1);
				int alpha = 255;
				switch (p) {
					case FABRIC: {
						lum += (y / 2) % 2 == 0 ? 4 : -4;
						if ((x + (y / 2) * 2) % 4 == 0) lum -= 6;
						lum += noise(seed + 2, x, y * 3, 5) * 4;
						break;
					}
					case FEATHER: {
						int fx = (x + col0) % 6;
						lum += fx == 0 ? -24 : fx == 1 ? 10 : fx == 2 ? 4 : fx == 5 ? -10 : 0;
						if ((fx + y) % 3 == 0) lum -= 5;
						if (side && y >= rh - 3 && fx >= 4) alpha = 0;
						lum -= y * 22f / Math.max(rh, 1);
						break;
					}
					case MEMBRANE: {
						lum -= 28 + y * 25f / Math.max(rh, 1);
						int v1 = Math.floorMod(x + col0, 11), v2 = Math.floorMod(x + y + col0, 17);
						if (v1 == 0 || v2 == 0) lum += 32;
						else if (v1 == 1 || v1 == 10 || v2 == 1 || v2 == 16) lum += 12;
						break;
					}
					case METAL: {
						float d = Math.floorMod(x + y, 16) - 2.5f;
						lum += 30 * (float) Math.exp(-d * d / 3) - 6;
						lum += noise(seed + 3, x * 4, y, 6) * 3;
						break;
					}
					case FUR: {
						float strand = noise(seed + 4, x * 3, y * 0.6f, 3);
						lum += strand * 16;
						if (((x * 7) + (y / 3) * 13 + col0) % 5 == 0) lum -= 10;
						break;
					}
					case LEATHER: {
						boolean stitch = (x == 3 || y == 3 || x == rw - 4 || y == rh - 4) && (x + y) % 4 < 2 && rw > 8 && rh > 8;
						if (stitch) lum += 26;
						lum -= 12 - noise(seed + 5, x, y, 3) * 6;
						break;
					}
					case GLASS: {
						float d = Math.floorMod(x + y, 14) - 1.5f;
						lum = 55 + 70 * (float) Math.exp(-d * d / 2.5f) + y * 18f / Math.max(rh, 1);
						break;
					}
					case STRIPES: {
						float s = Math.floorMod(x + y, 12);
						lum += 10 - 44 * (smooth(4.5f, 6.5f, s) - smooth(10.5f, 12, s));
						break;
					}
					case SPOTS: {
						float cx = Math.floorMod(x, 10) - 4.5f, cy = Math.floorMod(y, 10) - 4.5f;
						float r = (float) Math.sqrt(cx * cx + cy * cy);
						lum += 22 * (1 - smooth(2.2f, 3f, r)) - 34 * (smooth(2.4f, 3f, r) - smooth(3.4f, 4.2f, r));
						break;
					}
					case RIDGED: {
						int r = y % 4;
						lum += r == 0 ? -22 : r == 1 ? 8 : r == 3 ? -6 : 0;
						break;
					}
					case PLAIN: break;
				}
				int edge = Math.min(Math.min(x, y), Math.min(rw - 1 - x, rh - 1 - y));
				float rim = p == Pattern.METAL ? 46 : 34;
				if (thin < 6) rim *= 0.6f;
				if (edge == 0) lum -= rim;
				else if (edge == 1 && thin >= 6) lum -= rim * 0.35f;
				else if (edge == 2 && thin >= 10) lum -= rim * 0.12f;
				if (side && y >= 1 && y <= 2 && thin >= 6) lum += y == 1 ? 12 : 5;
				put(img, px, py, alpha << 24 | colour(lum, seed, x, y));
			}
		}
	}

	private static int colour(float lum, int seed, int x, int y) {
		float t = Math.max(-1, Math.min(1, (lum - 190) / 70));
		float r = lum * (t < 0 ? 1 + 0.12f * t : 1);
		float g = lum * (t < 0 ? 1 + 0.06f * t : 1 - 0.02f * t);
		float b = lum * (t < 0 ? 1 : 1 - 0.09f * t);
		r += noise(seed + 11, x, y, 9) * 5;
		g += noise(seed + 12, x, y, 9) * 4;
		b += noise(seed + 13, x, y, 9) * 5;
		return clamp(r) << 16 | clamp(g) << 8 | clamp(b);
	}

	private static int clamp(float v) {
		return Math.max(0, Math.min(255, Math.round(v)));
	}

	private static void put(NativeImage img, int x, int y, int argb) {
		img.setPixelRGBA(x, y, argb & 0xFF00FF00 | argb >> 16 & 0xFF | (argb & 0xFF) << 16);
	}
}
