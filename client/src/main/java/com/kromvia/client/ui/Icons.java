package com.kromvia.client.ui;

import com.kromvia.client.compat.Gfx;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class Icons {
	private static final int[] SIZES = {16, 20, 24, 32, 40, 48, 60, 64, 72, 96, 120, 144};
	private static final Map<String, Integer> INDEX = new HashMap<>();
	private static final Identifier[] TEXTURES = new Identifier[SIZES.length];
	private static int cols, rows;

	static {
		try (InputStream in = Icons.class.getResourceAsStream("/assets/kromvia/textures/gui/icons/index.txt")) {
			if (in != null) {
				BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
				cols = Integer.parseInt(r.readLine().trim());
				String line;
				while ((line = r.readLine()) != null) {
					if (!line.trim().isEmpty()) INDEX.put(line.trim(), INDEX.size());
				}
				rows = (INDEX.size() + cols - 1) / cols;
			}
		} catch (Exception ignored) {
			INDEX.clear();
		}
		for (int i = 0; i < SIZES.length; i++) TEXTURES[i] = Identifier.fromNamespaceAndPath("kromvia", "textures/gui/icons/" + SIZES[i] + ".png");
	}

	private Icons() {}

	public static boolean has(String name) {
		return name != null && INDEX.containsKey(name);
	}

	public static void draw(Gfx g, String name, int x, int y, int size, int color) {
		Integer i = INDEX.get(name);
		if (i == null) return;
		double scale = (double) Minecraft.getInstance().getWindow().getGuiScale();
		int want = (int) Math.round(size * scale);
		int pick = SIZES.length - 1;
		for (int s = 0; s < SIZES.length; s++) {
			if (SIZES[s] >= want) {
				pick = s;
				break;
			}
		}
		int px = SIZES[pick];
		g.blit(TEXTURES[pick], x, y, (i % cols) * px, (i / cols) * px, size, size, px, px, cols * px, rows * px, color);
	}
}
