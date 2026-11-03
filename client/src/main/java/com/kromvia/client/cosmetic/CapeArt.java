package com.kromvia.client.cosmetic;

import com.mojang.blaze3d.platform.NativeImage;

final class CapeArt {
	static final int RES = 10;
	static final int FRAMES = 32;
	static final int W = 22, H = 17;

	private record Palette(int[] body, int trim, int trimDark, int[] mark) {}

	private static final String[] MARK = {"11...11", "22..112", "22.112.", "22122..", "22.223.", "22..223", "33...33"};

	private CapeArt() {}

	private static Palette palette(String style) {
		return switch (style) {
			case "ember" -> new Palette(new int[] {0xC2412D, 0xB13A28, 0x9E3323, 0x8A2C1F, 0x76251A, 0x621E15}, 0xF2C94C, 0xE0A53C,
					new int[] {0xF2C94C, 0xF08A24, 0x4A1610});
			case "frost" -> new Palette(new int[] {0x4F8FD9, 0x4683CC, 0x3E77BD, 0x366AAC, 0x2E5D9A, 0x265088}, 0xE6F4FF, 0xB9DCF5,
					new int[] {0xE6F4FF, 0x9FD3F5, 0x1C3A66});
			default -> new Palette(new int[] {0xA35FD6, 0x8A5CE8, 0x6A5CF0, 0x4F5EE6, 0x3460DC, 0x1F63D6}, 0xC9B5FF, 0x8B5CF6,
					new int[] {0xE4DCFF, 0xC9B5FF, 0x2F55C8});
		};
	}

	static NativeImage[] frames(String style) {
		Palette p = palette(style);
		NativeImage[] out = new NativeImage[FRAMES];
		for (int f = 0; f < FRAMES; f++) {
			NativeImage img = new NativeImage(W * RES, H * RES, true);
			float t = f / (float) FRAMES;
			for (int ty = 0; ty < H * RES; ty++) {
				for (int tx = 0; tx < W * RES; tx++) {
					int c = texel(p, tx, ty, (tx + 0.5f) / RES, (ty + 0.5f) / RES, t);
					if (c != 0) Tex.put(img, tx, ty, c);
				}
			}
			out[f] = img;
		}
		return out;
	}

	private static int texel(Palette p, int tx, int ty, float u, float v, float t) {
		if (v < 1) {
			if (u >= 1 && u < 21) return 0xFF000000 | p.trim;
			return 0;
		}
		if (v >= 17) return 0;
		if (u < 1 || u >= 11 && u < 12) return 0xFF000000 | shade(p.trimDark, 0.9f);
		if (u < 11) return face(p, tx, ty, u - 1, v - 1, true, t);
		if (u < 22) return face(p, tx, ty, u - 12, v - 1, false, t);
		return 0;
	}

	private static int face(Palette p, int tx, int ty, float x, float y, boolean outside, float t) {
		double wave = Math.PI * 2 * t;
		int c = gradient(p.body, y / 16);
		float folds = smooth(1.5f, 4, y);
		float light = 1;
		for (float[] fold : new float[][] {{3, 0}, {6.5f, 1.7f}}) {
			float centre = fold[0] + 0.4f * (float) Math.sin(wave + fold[1]) * folds;
			float d = x - centre;
			light -= 0.16f * folds * (float) Math.exp(-d * d / 0.6f);
			float r = d - 0.9f;
			light += 0.07f * folds * (float) Math.exp(-r * r / 0.4f);
		}
		light *= ((tx >> 1) + (ty >> 1)) % 2 == 0 ? 1.025f : 0.975f;
		light += 0.03f * Tex.noise(77, tx, ty, 6);
		if (!outside) light *= 0.72f;

		boolean border = x < 0.7f || x > 9.3f || y < 0.8f || y > 15.2f;
		if (border) {
			c = y < 0.8f ? p.trim : y > 15.2f ? ((int) (x * 2) % 2 == 0 ? p.trim : p.trimDark) : p.trimDark;
			if (!outside) light = 0.8f;
			else light = 1 - 0.12f * Math.min(Math.min(Math.abs(x - 0.35f), Math.abs(x - 9.65f)), 0.35f) / 0.35f;
		} else if (outside) {
			int emblem = emblem(p, x, y, t);
			if (emblem != 0) {
				c = emblem;
				light = 1;
			} else {
				float glow = halo(x, y) * (0.28f + 0.16f * (float) Math.sin(wave));
				c = mix(c, p.mark[1], glow);
			}
		}
		c = shade(c, light);
		if (outside) {
			float q = x * 0.6f + y * 0.8f;
			float pos = -4 + t * 26.8f;
			float sweep = (float) Math.exp(-Math.pow((q - pos) / 1.3f, 2)) * 0.32f;
			c = mix(c, 0xFFFFFF, sweep);
		}
		return 0xFF000000 | c;
	}

	private static int emblem(Palette p, float x, float y, float t) {
		int cx = (int) Math.floor(x - 1), cy = (int) Math.floor(y - 4);
		if (cx < 0 || cx >= 7 || cy < 0 || cy >= 7) return 0;
		char m = MARK[cy].charAt(cx);
		if (m == '.') return 0;
		float fx = x - 1 - cx, fy = y - 4 - cy;
		int c = p.mark[m - '1'];
		if (fx < 0.14f || fy < 0.14f) c = mix(c, 0xFFFFFF, 0.18f);
		else if (fx > 0.86f || fy > 0.86f) c = shade(c, 0.82f);
		c = shade(c, 1.04f - 0.08f * (y - 4) / 7);
		return mix(c, 0xFFFFFF, 0.16f * (0.5f + 0.5f * (float) Math.sin(Math.PI * 2 * t)));
	}

	private static float halo(float x, float y) {
		float best = 9;
		for (int cy = 0; cy < 7; cy++) {
			for (int cx = 0; cx < 7; cx++) {
				if (MARK[cy].charAt(cx) == '.') continue;
				float dx = Math.max(0, Math.max(1 + cx - x, x - (2 + cx)));
				float dy = Math.max(0, Math.max(4 + cy - y, y - (5 + cy)));
				best = Math.min(best, (float) Math.sqrt(dx * dx + dy * dy));
			}
		}
		return (float) Math.exp(-best * 2.4f);
	}

	private static int gradient(int[] cs, float f) {
		float i = Math.max(0, Math.min(1, f)) * (cs.length - 1);
		int a = Math.min(cs.length - 2, (int) i);
		return mix(cs[a], cs[a + 1], i - a);
	}

	private static float smooth(float e0, float e1, float v) {
		float t = Math.max(0, Math.min(1, (v - e0) / (e1 - e0)));
		return t * t * (3 - 2 * t);
	}

	private static int mix(int a, int b, float f) {
		f = Math.max(0, Math.min(1, f));
		int r = Math.round((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * f);
		int g = Math.round((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * f);
		int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * f);
		return r << 16 | g << 8 | bl;
	}

	private static int shade(int c, float m) {
		int r = Math.min(255, Math.round((c >> 16 & 255) * m));
		int g = Math.min(255, Math.round((c >> 8 & 255) * m));
		int b = Math.min(255, Math.round((c & 255) * m));
		return r << 16 | g << 8 | b;
	}
}
