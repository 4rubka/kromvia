package com.kromvia.client.cosmetic;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

final class DevCape {
	static final int RES = 20;
	static final int FRAMES = 40;
	private static final int W = CapeArt.W * RES, H = CapeArt.H * RES;
	private static final float MARK_TOP = 4.2f, MARK_H = 7.6f;
	private static final int[] TOP = {0x1B1240, 0x170F38}, BOTTOM = {0x0B1C52, 0x08143A};
	private static final int VIOLET = 0xA35FD6, BLUE = 0x3A7BFF, PINK = 0xE07AD6, TRIM = 0xE4DCFF, GOLD = 0xFFD98A;

	private DevCape() {}

	static NativeImage[] frames() {
		float[][] mark = mark();
		float[] glow = blur(mark[3], markW(), markH(), RES);
		NativeImage[] out = new NativeImage[FRAMES];
		for (int f = 0; f < FRAMES; f++) {
			float t = f / (float) FRAMES;
			NativeImage img = new NativeImage(W, H, true);
			for (int ty = 0; ty < H; ty++) {
				for (int tx = 0; tx < W; tx++) {
					int c = texel(tx, ty, t, mark, glow);
					if (c != 0) Tex.put(img, tx, ty, c);
				}
			}
			out[f] = img;
		}
		return out;
	}

	private static int markH() {
		return Math.round(MARK_H * RES);
	}

	private static int markW() {
		return Math.round(MARK_H * RES * 149 / 256f);
	}

	private static float[][] mark() {
		int w = markW(), h = markH();
		float[][] out = new float[4][w * h];
		try (InputStream in = DevCape.class.getResourceAsStream("/assets/kromvia/capes/dev_mark.png")) {
			BufferedImage src = ImageIO.read(in);
			BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = dst.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.drawImage(src, 0, 0, w, h, null);
			g.dispose();
			for (int i = 0; i < w * h; i++) {
				int p = dst.getRGB(i % w, i / w);
				out[0][i] = p >> 16 & 255;
				out[1][i] = p >> 8 & 255;
				out[2][i] = p & 255;
				out[3][i] = (p >>> 24) / 255f;
			}
		} catch (Exception | Error e) {
			return out;
		}
		return out;
	}

	private static float[] blur(float[] a, int w, int h, int r) {
		int pad = r * 2;
		int bw = w + pad * 2, bh = h + pad * 2;
		float[] img = new float[bw * bh];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) img[(y + pad) * bw + x + pad] = a[y * w + x];
		}
		for (int pass = 0; pass < 3; pass++) {
			img = box(img, bw, bh, r / 2, true);
			img = box(img, bw, bh, r / 2, false);
		}
		return img;
	}

	private static float[] box(float[] a, int w, int h, int r, boolean horizontal) {
		float[] out = new float[a.length];
		int n = horizontal ? w : h, lines = horizontal ? h : w;
		for (int l = 0; l < lines; l++) {
			float sum = 0;
			for (int i = -r; i <= r; i++) sum += at(a, w, horizontal, l, i, n);
			for (int i = 0; i < n; i++) {
				out[horizontal ? l * w + i : i * w + l] = sum / (2 * r + 1);
				sum += at(a, w, horizontal, l, i + r + 1, n) - at(a, w, horizontal, l, i - r, n);
			}
		}
		return out;
	}

	private static float at(float[] a, int w, boolean horizontal, int line, int i, int n) {
		if (i < 0 || i >= n) return 0;
		return horizontal ? a[line * w + i] : a[i * w + line];
	}

	private static int texel(int tx, int ty, float t, float[][] mark, float[] glow) {
		float u = (tx + 0.5f) / RES, v = (ty + 0.5f) / RES;
		if (v < 1) return u >= 1 && u < 21 ? 0xFF000000 | edge(u, v, t) : 0;
		if (v >= 17) return 0;
		if (u < 1 || u >= 11 && u < 12) return 0xFF000000 | edge(u, v, t);
		if (u < 11) return 0xFF000000 | face(tx - RES, ty - RES, u - 1, v - 1, true, t, mark, glow);
		if (u < 22) return 0xFF000000 | face(tx - 12 * RES, ty - RES, u - 12, v - 1, false, t, mark, glow);
		return 0;
	}

	private static int edge(float u, float v, float t) {
		float shimmer = 0.5f + 0.5f * (float) Math.sin(Math.PI * 2 * (t + (u + v) / 12));
		return mix(mix(VIOLET, BLUE, v / 17), TRIM, 0.25f + 0.25f * shimmer);
	}

	private static int face(int px, int py, float x, float y, boolean outside, float t, float[][] mark, float[] glow) {
		double wave = Math.PI * 2 * t;
		float fy = y / 16;
		int c = mix(mix(TOP[0], BOTTOM[0], fy), mix(TOP[1], BOTTOM[1], fy), x / 10);
		if (!outside) return shade(mix(c, VIOLET, 0.12f + 0.06f * (float) Math.sin(wave + y * 0.4f)), 0.75f);

		for (int i = 0; i < 3; i++) {
			float phase = i * 2.1f;
			float centre = 3 + i * 4.6f + 1.1f * (float) Math.sin(x * 0.55f + wave + phase) + 0.5f * (float) Math.sin(x * 1.3f - wave * 2 + phase);
			float d = (y - centre) / (1.1f + 0.35f * (float) Math.sin(x * 0.8f + wave + phase));
			float band = (float) Math.exp(-d * d) * (0.55f + 0.2f * (float) Math.sin(wave + phase));
			int tint = i == 0 ? VIOLET : i == 1 ? BLUE : PINK;
			c = add(c, tint, band * 0.55f);
		}

		c = add(c, 0xFFFFFF, stars(x, y, t));

		int mw = markW(), mh = markH();
		int mx = px - Math.round((10 - MARK_H * 149 / 256f) / 2 * RES), my = py - Math.round(MARK_TOP * RES);
		int pad = RES * 2, bw = mw + pad * 2;
		int gx = mx + pad, gy = my + pad;
		if (gx >= 0 && gx < bw && gy >= 0 && gy < mh + pad * 2) {
			float g = glow[gy * bw + gx] * (0.75f + 0.25f * (float) Math.sin(wave));
			c = add(c, VIOLET, g * 0.9f);
		}
		if (mx >= 0 && mx < mw && my >= 0 && my < mh) {
			int i = my * mw + mx;
			float a = mark[3][i];
			if (a > 0) {
				int k = Math.round(mark[0][i]) << 16 | Math.round(mark[1][i]) << 8 | Math.round(mark[2][i]);
				k = mix(k, 0xFFFFFF, 0.12f + 0.1f * (float) Math.sin(wave));
				c = mix(c, k, a);
			}
		}

		float aa = 1f / RES;
		float inset = Math.min(Math.min(x, 10 - x), Math.min(y, 16 - y));
		float line = smooth(0.32f - aa, 0.32f, inset) * (1 - smooth(0.52f, 0.52f + aa, inset));
		c = mix(c, mix(TRIM, GOLD, 0.5f + 0.5f * (float) Math.sin(wave + (x + y) * 0.3f)), line * 0.9f);
		c = mix(c, mix(VIOLET, BLUE, y / 16), 1 - smooth(0, 0.3f, inset));

		float q = x * 0.6f + y * 0.8f;
		float pos = -4 + t * 26.8f;
		float sweep = (float) Math.exp(-Math.pow((q - pos) / 1.6f, 2)) * 0.22f;
		return add(c, 0xFFFFFF, sweep);
	}

	private static float stars(float x, float y, float t) {
		float best = 0;
		int cx = (int) Math.floor(x / 1.25f), cy = (int) Math.floor(y / 1.25f);
		for (int oy = -1; oy <= 1; oy++) {
			for (int ox = -1; ox <= 1; ox++) {
				int gx = cx + ox, gy = cy + oy;
				int h = hash(gx, gy);
				if ((h & 3) != 0) continue;
				float sx = (gx + (h >> 4 & 255) / 255f) * 1.25f;
				float sy = (gy + (h >> 12 & 255) / 255f) * 1.25f;
				float d2 = (x - sx) * (x - sx) + (y - sy) * (y - sy);
				float twinkle = 0.5f + 0.5f * (float) Math.sin(Math.PI * 2 * (t * 2 + (h >> 20 & 255) / 255f));
				best = Math.max(best, (float) Math.exp(-d2 / 0.006f) * (0.4f + 0.6f * twinkle));
			}
		}
		return best;
	}

	private static int hash(int x, int y) {
		int h = x * 374761393 + y * 668265263;
		h = (h ^ h >>> 13) * 1274126177;
		return h ^ h >>> 16;
	}

	private static float smooth(float e0, float e1, float v) {
		float t = Math.max(0, Math.min(1, (v - e0) / (e1 - e0)));
		return t * t * (3 - 2 * t);
	}

	private static int add(int a, int b, float f) {
		f = Math.max(0, f);
		int r = Math.min(255, Math.round((a >> 16 & 255) + (b >> 16 & 255) * f));
		int g = Math.min(255, Math.round((a >> 8 & 255) + (b >> 8 & 255) * f));
		int bl = Math.min(255, Math.round((a & 255) + (b & 255) * f));
		return r << 16 | g << 8 | bl;
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
