package com.kromvia.client.compat;

import net.minecraft.util.FastColor;

public final class ARGB {
	private ARGB() {}

	public static int alpha(int argb) {
		return FastColor.ARGB32.alpha(argb);
	}

	public static int color(int alpha, int rgb) {
		return FastColor.ARGB32.color(alpha, rgb);
	}

	public static int color(int alpha, int r, int g, int b) {
		return FastColor.ARGB32.color(alpha, r, g, b);
	}

	public static int lerp(float t, int from, int to) {
		return FastColor.ARGB32.lerp(t, from, to);
	}

	public static int white(float alpha) {
		return FastColor.ARGB32.color(FastColor.as8BitChannel(alpha), 0xFFFFFF);
	}
}
