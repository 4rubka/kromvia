package com.kromvia.client.compat;

import net.minecraft.util.FastColor;

public final class ARGB {
	private ARGB() {}

	public static int alpha(int argb) {
		return FastColor.ARGB32.alpha(argb);
	}

	public static int color(int alpha, int rgb) {
		return FastColor.ARGB32.color(alpha, rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF);
	}

	public static int color(int alpha, int r, int g, int b) {
		return FastColor.ARGB32.color(alpha, r, g, b);
	}

	public static int lerp(float t, int from, int to) {
		int a = FastColor.ARGB32.alpha(from) + (int) (t * (FastColor.ARGB32.alpha(to) - FastColor.ARGB32.alpha(from)));
		int r = FastColor.ARGB32.red(from) + (int) (t * (FastColor.ARGB32.red(to) - FastColor.ARGB32.red(from)));
		int g = FastColor.ARGB32.green(from) + (int) (t * (FastColor.ARGB32.green(to) - FastColor.ARGB32.green(from)));
		int b = FastColor.ARGB32.blue(from) + (int) (t * (FastColor.ARGB32.blue(to) - FastColor.ARGB32.blue(from)));
		return FastColor.ARGB32.color(a, r, g, b);
	}

	public static int white(float alpha) {
		return FastColor.ARGB32.color((int) Math.floor(alpha * 255), 255, 255, 255);
	}
}
