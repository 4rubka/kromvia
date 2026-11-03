package com.kromvia.client.ui;

import com.kromvia.client.compat.Gfx;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public final class Backgrounds {
	private static final long SHOW_MS = 12_000;
	private static final long FADE_MS = 1_500;

	private record Image(Identifier id, int w, int h) {}

	private static List<String> names;
	private static final Map<Integer, Image> loaded = new HashMap<>();

	private Backgrounds() {}

	private static List<String> names() {
		if (names != null) return names;
		names = new ArrayList<>();
		try (InputStream idx = Backgrounds.class.getResourceAsStream("/kromvia/backgrounds/index.txt")) {
			if (idx == null) return names;
			BufferedReader r = new BufferedReader(new InputStreamReader(idx, StandardCharsets.UTF_8));
			String line;
			while ((line = r.readLine()) != null) {
				if (!line.isBlank()) names.add(line.trim());
			}
		} catch (IOException ignored) {
		}
		return names;
	}

	private static Image get(int i) {
		Image img = loaded.get(i);
		if (img != null || loaded.containsKey(i)) return img;
		try (InputStream in = Backgrounds.class.getResourceAsStream("/kromvia/backgrounds/" + names.get(i))) {
			if (in != null) {
				NativeImage ni = NativeImage.read(in);
				Identifier id = Identifier.fromNamespaceAndPath("kromvia", "background/" + i);
				Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, ni));
				img = new Image(id, ni.getWidth(), ni.getHeight());
			}
		} catch (IOException ignored) {
		}
		loaded.put(i, img);
		return img;
	}

	private static void keepOnly(int a, int b) {
		loaded.entrySet().removeIf(e -> {
			if (e.getKey() == a || e.getKey() == b) return false;
			if (e.getValue() != null) Minecraft.getInstance().getTextureManager().release(e.getValue().id);
			return true;
		});
	}

	public static boolean render(Gfx g) {
		List<String> all = names();
		if (all.isEmpty()) return false;
		long now = System.currentTimeMillis();
		int i = (int) (now / SHOW_MS % all.size());
		int next = (i + 1) % all.size();
		keepOnly(i, next);
		Image cur = get(i);
		if (cur == null) return false;
		draw(g, cur, 1);
		long into = now % SHOW_MS;
		if (into > SHOW_MS - FADE_MS && all.size() > 1) {
			Image n = get(next);
			if (n != null) draw(g, n, (into - (SHOW_MS - FADE_MS)) / (float) FADE_MS);
		}
		g.nextStratum();
		Meteors.draw(g);
		return true;
	}

	private static void draw(Gfx g, Image img, float alpha) {
		int sw = g.guiWidth(), sh = g.guiHeight();
		float scale = Math.max(sw / (float) img.w, sh / (float) img.h);
		int rw = Math.round(sw / scale), rh = Math.round(sh / scale);
		int u = (img.w - rw) / 2, v = (img.h - rh) / 2;
		g.blit(img.id, 0, 0, u, v, sw, sh, rw, rh, img.w, img.h, ARGB.white(alpha));
	}
}
