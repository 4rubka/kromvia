package com.kromvia.client.ui;

import com.kromvia.client.compat.Gfx;
import java.io.IOException;
import java.io.InputStream;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class UiStyle {
	public static final int BG = 0xFF07080B;
	public static final int PANEL = 0xF6090B0E;
	public static final int CARD = 0xFF0F1216;
	public static final int LINE = 0xFF1A1E25;
	public static final int TOP = 0xFFB49CFF;
	public static final int FRONT = 0xFF8B5CF6;
	public static final int SIDE = 0xFF5B3FD6;
	public static final int TEXT = 0xFFF4F5F7;
	public static final int MUTED = 0xFF9AA1AD;

	private static final Identifier LOGO = Identifier.fromNamespaceAndPath("kromvia", "logo");
	private static boolean logoReady;

	private UiStyle() {}

	public static void button(Gfx g, int x, int y, int w, int h, boolean active, boolean lit) {
		int face, top, side;
		if (!active) {
			face = 0xFF15181E;
			top = 0xFF1B1F27;
			side = 0xFF0C0E12;
		} else if (lit) {
			face = SIDE;
			top = FRONT;
			side = 0xFF2E1F73;
		} else {
			face = 0xFF1C212A;
			top = 0xFF2C333F;
			side = 0xFF0E1116;
		}
		g.fill(x, y, x + w, y + h - 2, face);
		g.fill(x, y, x + w, y + 1, top);
		g.fill(x, y + h - 2, x + w, y + h, side);
	}

	public static void panel(Gfx g, int x, int y, int w, int h) {
		shadow(g, x, y, w, h, 10);
		g.fill(x, y, x + w, y + h, PANEL);
		g.fillGradient(x + 1, y + 1, x + w - 1, y + 40, 0x185B3FD6, 0x005B3FD6);
		g.fill(x, y, x + w, y + 1, LINE);
		g.fill(x, y + h - 1, x + w, y + h, LINE);
		g.fill(x, y, x + 1, y + h, LINE);
		g.fill(x + w - 1, y, x + w, y + h, LINE);
		int glow = w / 3;
		g.fill(x + (w - glow) / 2, y, x + (w + glow) / 2, y + 1, 0x998B5CF6);
	}

	public static void shadow(Gfx g, int x, int y, int w, int h, int r) {
		for (int i = r; i > 0; i--) {
			int a = Math.round(36F * (1 - i / (float) (r + 1)) / r * 3);
			g.fill(x - i, y - i + 3, x + w + i, y + h + i + 3, ARGB.color(a, 0));
		}
	}

	public static void dim(Gfx g, int w, int h, float t) {
		g.fill(0, 0, w, h, ARGB.color(Math.round(0xB0 * t), BG));
		int edge = Math.round(0x90 * t);
		g.fillGradient(0, 0, w, h / 3, ARGB.color(edge, 0), 0);
		g.fillGradient(0, h - h / 3, w, h, 0, ARGB.color(edge, 0));
	}

	public static float ease(float t) {
		t = Mth.clamp(t, 0, 1);
		return 1 - (1 - t) * (1 - t) * (1 - t);
	}

	public static float since(long since, float ms) {
		return ease((System.currentTimeMillis() - since) / ms);
	}

	public static int fade(int argb, float a) {
		return ARGB.color(Math.round(ARGB.alpha(argb) * Mth.clamp(a, 0, 1)), argb);
	}

	public static void logo(Gfx g, int x, int y, int size) {
		if (!logoReady) {
			try (InputStream in = UiStyle.class.getResourceAsStream("/kromvia/logo.png")) {
				if (in != null) Minecraft.getInstance().getTextureManager().register(LOGO, new DynamicTexture(LOGO::toString, NativeImage.read(in)));
			} catch (IOException ignored) {
			}
			logoReady = true;
		}
		g.blit(LOGO, x, y, 0, 0, size, size, 128, 128, 128, 128);
	}

	public static java.util.List<String> wrap(net.minecraft.client.gui.Font font, String text, int width) {
		java.util.List<String> lines = new java.util.ArrayList<>();
		if (text.isEmpty()) return lines;
		String line = "";
		for (String word : text.split(" ")) {
			String next = line.isEmpty() ? word : line + " " + word;
			if (font.width(next) > width && !line.isEmpty()) {
				lines.add(line);
				line = word;
			} else {
				line = next;
			}
		}
		lines.add(line);
		return lines;
	}
}
