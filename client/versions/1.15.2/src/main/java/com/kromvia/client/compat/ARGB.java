package com.kromvia.client.compat;

public final class ARGB {
	private ARGB() {}

	public static int alpha(int argb) {
		return argb >>> 24;
	}

	public static int color(int alpha, int rgb) {
		return alpha << 24 | rgb & 0xFFFFFF;
	}

	public static int color(int alpha, int r, int g, int b) {
		return alpha << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
	}

	public static int lerp(float t, int from, int to) {
		int a = alpha(from) + (int) (t * (alpha(to) - alpha(from)));
		int r = (from >> 16 & 0xFF) + (int) (t * ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)));
		int g = (from >> 8 & 0xFF) + (int) (t * ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)));
		int b = (from & 0xFF) + (int) (t * ((to & 0xFF) - (from & 0xFF)));
		return color(a, r, g, b);
	}

	public static int white(float alpha) {
		return color((int) Math.floor(alpha * 255), 255, 255, 255);
	}
}
