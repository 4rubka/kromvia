package com.kromvia.client.cosmetic;

import java.util.Random;

import com.mojang.blaze3d.platform.NativeImage;

final class Tex {
	enum Face { TOP, BOTTOM, WEST, NORTH, EAST, SOUTH }

	private Tex() {}

	private static int base(Face f) {
		return switch (f) {
			case TOP -> 238;
			case NORTH -> 218;
			case SOUTH -> 206;
			case WEST, EAST -> 198;
			case BOTTOM -> 170;
		};
	}

	private static float hash(int seed, int x, int y) {
		int h = seed ^ x * 374761393 ^ y * 668265263;
		h = (h ^ h >>> 13) * 1274126177;
		return ((h ^ h >>> 16) & 0xFFFF) / 32767.5f - 1;
	}

	static float noise(int seed, float x, float y, float cell) {
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
		float k = Shapes.SCALE / 4f;
		float fw = rw / k, fh = rh / k;
		int iw = Math.round(fw), ih = Math.round(fh);
		float thin = Math.min(fw, fh);
		for (int y = 0; y < rh; y++) {
			for (int x = 0; x < rw; x++) {
				int px = rx + x, py = ry + y;
				if (px >= img.getWidth() || py >= img.getHeight()) continue;
				float u = (x + 0.5f) / k, v = (y + 0.5f) / k;
				int ix = (int) u, iy = (int) v;
				float lum = base(f) + noise(seed, u, v, 7) * 9 + noise(seed + 1, u, v, 2.5f) * 4 + noise(seed + 6, x, y, 2.2f) * 3;
				if (side) lum += 8 - 16f * v / Math.max(fh - 1, 1);
				int alpha = 255;
				switch (p) {
					case FABRIC -> {
						lum += (iy / 2) % 2 == 0 ? 4 : -4;
						if ((ix + (iy / 2) * 2) % 4 == 0) lum -= 6;
						lum += noise(seed + 2, u, v * 3, 5) * 4;
						lum += ((x + y) % 3 == 0 ? -3 : 1) + noise(seed + 7, x, y * 3, 2) * 3;
					}
					case FEATHER -> {
						int fx = (ix + col0) % 6;
						lum += switch (fx) {
							case 0 -> -24;
							case 1 -> 10;
							case 2 -> 4;
							case 5 -> -10;
							default -> 0;
						};
						if ((fx + iy) % 3 == 0) lum -= 5;
						float barb = Math.floorMod(x + y, Math.max(2, (int) k)) == 0 ? -6 : 0;
						lum += barb;
						if (side && v >= fh - 3 && fx >= 4) alpha = 0;
						lum -= v * 22f / Math.max(fh, 1);
					}
					case MEMBRANE -> {
						lum -= 28 + v * 25f / Math.max(fh, 1);
						float v1 = fmod(u + col0, 11), v2 = fmod(u + v + col0, 17);
						float vein = Math.max(1 - smooth(0, 0.6f, Math.min(v1, 11 - v1)), 1 - smooth(0, 0.6f, Math.min(v2, 17 - v2)));
						float halo = Math.max(1 - smooth(0.6f, 1.6f, Math.min(v1, 11 - v1)), 1 - smooth(0.6f, 1.6f, Math.min(v2, 17 - v2)));
						lum += vein * 32 + (halo - vein) * 12;
					}
					case METAL -> {
						float d = fmod(u + v, 16) - 2.5f;
						lum += 30 * (float) Math.exp(-d * d / 3) - 6;
						lum += noise(seed + 3, u * 4, v, 6) * 3 + noise(seed + 9, x * 6, y, 3) * 2;
					}
					case FUR -> {
						float strand = noise(seed + 4, u * 3, v * 0.6f, 3);
						lum += strand * 16 + noise(seed + 8, x * 2.5f, y * 0.4f, 2) * 8;
						if (((ix * 7) + (iy / 3) * 13 + col0) % 5 == 0) lum -= 10;
					}
					case LEATHER -> {
						float sx = Math.min(Math.abs(u - 3.5f), Math.abs(u - (fw - 3.5f))), sy = Math.min(Math.abs(v - 3.5f), Math.abs(v - (fh - 3.5f)));
						boolean onLine = (sx < 0.3f && v > 3 && v < fh - 3) || (sy < 0.3f && u > 3 && u < fw - 3);
						boolean stitch = onLine && fmod(u + v, 4) < 2 && iw > 8 && ih > 8;
						if (stitch) lum += 26;
						lum -= 12 - noise(seed + 5, u, v, 3) * 6 - noise(seed + 10, x, y, 1.5f) * 3;
					}
					case GLASS -> {
						float d = fmod(u + v, 14) - 1.5f;
						lum = 55 + 70 * (float) Math.exp(-d * d / 2.5f) + v * 18f / Math.max(fh, 1);
					}
					case STRIPES -> {
						float s = fmod(u + v, 12);
						lum += 10 - 44 * (smooth(4.5f, 6.5f, s) - smooth(10.5f, 12, s));
					}
					case SPOTS -> {
						float cx = fmod(u, 10) - 5f, cy = fmod(v, 10) - 5f;
						float r = (float) Math.sqrt(cx * cx + cy * cy);
						lum += 22 * (1 - smooth(2.2f, 3f, r)) - 34 * (smooth(2.4f, 3f, r) - smooth(3.4f, 4.2f, r));
					}
					case RIDGED -> {
						int r = iy % 4;
						lum += r == 0 ? -22 : r == 1 ? 8 : r == 3 ? -6 : 0;
					}
					case PLAIN -> {}
				}
				float edge = Math.min(Math.min(u, v), Math.min(fw - u, fh - v));
				float rim = p == Pattern.METAL ? 46 : 34;
				if (thin < 6) rim *= 0.6f;
				if (edge < 1) lum -= rim * (1 - 0.5f * smooth(0.4f, 1, edge));
				else if (edge < 2 && thin >= 6) lum -= rim * 0.35f;
				else if (edge < 3 && thin >= 10) lum -= rim * 0.12f;
				if (side && v >= 1 && v < 3 && thin >= 6) lum += v < 2 ? 12 : 5;
				put(img, px, py, alpha << 24 | colour(lum, seed, u, v));
			}
		}
	}

	private static float fmod(float v, float m) {
		return v - m * (float) Math.floor(v / m);
	}

	private static int colour(float lum, int seed, float x, float y) {
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

	static void put(NativeImage img, int x, int y, int argb) {
		img.setPixel(x, y, argb);
	}
}
