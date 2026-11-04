package com.kromvia.client.cosmetic;

import com.mojang.blaze3d.platform.NativeImage;

/** The Kromvia cape, pixel for pixel the same as the launcher's and the site's (src/art/capes.ts). */
final class CapeArt {
	static final int FRAMES = 1;
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
		NativeImage img = new NativeImage(W, H, true);
		long[] seed = {style.length() * 977L};
		for (int ox : new int[] {1, 12}) {
			boolean outside = ox == 1;
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 10; x++) {
					double f = y / 16.0 * (p.body.length - 1);
					int i = Math.min(p.body.length - 1, (int) Math.floor(f + (next(seed) < f % 1 ? 1 : 0)));
					if ((x == 3 || x == 6) && y > 2) i = Math.min(p.body.length - 1, i + 1);
					if (!outside) i = Math.min(p.body.length - 1, i + 2);
					put(img, ox + x, 1 + y, p.body[i]);
				}
			}
			for (int x = 0; x < 10; x++) {
				put(img, ox + x, 1, p.trim);
				put(img, ox + x, 16, x % 2 == 1 ? p.trim : p.trimDark);
			}
			for (int y = 1; y < 17; y++) {
				put(img, ox, y, p.trimDark);
				put(img, ox + 9, y, p.trimDark);
			}
			if (outside) {
				for (int y = 0; y < MARK.length; y++) {
					for (int x = 0; x < MARK[y].length(); x++) {
						char m = MARK[y].charAt(x);
						if (m != '.') put(img, ox + 1 + x, 5 + y, p.mark[m - '1']);
					}
				}
			}
		}
		for (int x = 1; x < 21; x++) put(img, x, 0, p.trim);
		for (int y = 1; y < 17; y++) {
			put(img, 0, y, p.trimDark);
			put(img, 11, y, p.trimDark);
		}
		return new NativeImage[] {img};
	}

	/** The launcher's Park-Miller generator, so the shading dither matches it pixel for pixel. */
	private static double next(long[] seed) {
		seed[0] = seed[0] * 16807 % 2147483647;
		return seed[0] / 2147483647.0;
	}

	private static void put(NativeImage img, int x, int y, int rgb) {
		Tex.put(img, x, y, 0xFF000000 | rgb);
	}
}
