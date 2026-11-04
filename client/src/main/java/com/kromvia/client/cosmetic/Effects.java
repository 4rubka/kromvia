package com.kromvia.client.cosmetic;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kromvia.client.cosmetic.Cosmetic.Anchor;
import com.kromvia.client.cosmetic.Cosmetic.Animator;
import com.kromvia.client.cosmetic.Cosmetic.Piece;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Effects are the launcher's and the site's pixel icons (src/art/effects.json) floating around the
 * player. They are drawn with the cosmetics layer, so the wardrobe preview shows them too.
 */
final class Effects {
	private static final Logger LOG = LoggerFactory.getLogger("kromvia");
	private static final int SPRITES = 6;
	private static final float PERIOD = 50;
	private static final float PI = (float) Math.PI, TWO_PI = PI * 2;
	private static final Set<String> FALLING = Set.of("snow", "petals", "ash", "rain_cloud", "spores");
	private static final Set<String> CIRCLING = Set.of("aura", "glow", "portal", "enchant", "witch", "rainbow");
	private static JsonObject icons;

	private enum Motion { RISE, FALL, ORBIT }

	private Effects() {}

	static List<Piece> pieces(String id) {
		JsonObject sprite = icons().getAsJsonObject(id);
		if (sprite == null) return List.of();
		Motion motion = FALLING.contains(id) ? Motion.FALL : CIRCLING.contains(id) ? Motion.ORBIT : Motion.RISE;
		List<Piece> out = new ArrayList<>();
		for (int i = 0; i < SPRITES; i++) {
			Shapes.Built built = Shapes.sprite(() -> new NativeImage[] {icon(sprite)});
			out.add(new Piece(id + "_fx" + i, built, Anchor.BODY, 0xFFFFFFFF, true, animator(motion, i / (float) SPRITES)));
		}
		return out;
	}

	private static JsonObject icons() {
		if (icons != null) return icons;
		try (InputStream in = Effects.class.getResourceAsStream("/kromvia/effects.json")) {
			icons = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
		} catch (IOException | RuntimeException e) {
			LOG.error("Could not read the effect icons", e);
			icons = new JsonObject();
		}
		return icons;
	}

	/** The 9x9 icon twice side by side: the front and the back face of the sprite's plane. */
	private static NativeImage icon(JsonObject sprite) {
		var rows = sprite.getAsJsonArray("rows");
		JsonObject colors = sprite.getAsJsonObject("colors");
		NativeImage img = new NativeImage(18, 9, true);
		for (int y = 0; y < rows.size(); y++) {
			String row = rows.get(y).getAsString();
			for (int x = 0; x < row.length(); x++) {
				String key = String.valueOf(row.charAt(x));
				if (!colors.has(key)) continue;
				int rgb = Integer.parseInt(colors.get(key).getAsString().substring(1), 16);
				Tex.put(img, x, y, 0xFF000000 | rgb);
				Tex.put(img, 17 - x, y, 0xFF000000 | rgb);
			}
		}
		return img;
	}

	private static Animator animator(Motion motion, float phase) {
		float spread = phase * 5.3f % 1;
		return (p, s) -> {
			float t = (s.ageInTicks / PERIOD + phase) % 1;
			float angle, radius, scale;
			switch (motion) {
				case FALL -> {
					angle = phase * TWO_PI;
					radius = 3 + 6 * spread;
					p.y = -16 + t * 38;
					scale = Math.min(1, t * 8) * Math.min(1, (1 - t) * 4);
				}
				case ORBIT -> {
					angle = (phase + t) * TWO_PI;
					radius = 9;
					p.y = 20 + Mth.sin((t + phase) * TWO_PI * 2);
					scale = 0.9f;
				}
				default -> {
					angle = phase * TWO_PI + t * PI;
					radius = 7 + 2 * spread;
					p.y = 22 - t * 32;
					scale = Mth.sin(t * PI);
				}
			}
			p.x = Mth.cos(angle) * radius;
			p.z = Mth.sin(angle) * radius;
			p.yRot = -angle;
			p.xRot = 0;
			PartScale.set(p, scale);
		};
	}
}
