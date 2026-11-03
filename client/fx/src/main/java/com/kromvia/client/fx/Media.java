package com.kromvia.client.fx;

import com.google.gson.JsonObject;
import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.Mc;
import com.kromvia.client.module.Module;
import com.kromvia.client.module.setting.BoolSetting;
import com.kromvia.client.module.setting.ColorSetting;
import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Media extends Module {
	private static final Logger PRESENCE = LoggerFactory.getLogger("kromvia-presence");
	private static final int KP_4 = 324, KP_5 = 325, KP_6 = 326;
	private static final int ART = 32, W = 150, H = ART + 6;

	private record Track(String title, String artist, boolean playing, long position, long length, String art, long at) {}

	private final BoolSetting cover = add(new BoolSetting("cover", "Cover art", true));
	private final BoolSetting bar = add(new BoolSetting("bar", "Progress bar", true));
	private final BoolSetting keys = add(new BoolSetting("keys", "Numpad 4 / 5 / 6: previous, pause, next", true));
	private final ColorSetting accent = add(new ColorSetting("accent", "Bar colour", 0xFF8B5CF6));

	private volatile Track track;
	private String artPath;
	private CompletableFuture<BufferedImage> artLoading;
	private Identifier artId;
	private final boolean[] held = new boolean[3];

	Media() {
		super("music", "Music", Category.HUD, Corner.TOP_RIGHT, false);
		icon(Items.MUSIC_DISC_CAT);
	}

	static final Media INSTANCE = new Media();

	static void set(JsonObject m) {
		INSTANCE.track = m == null ? null : new Track(
				m.get("title").getAsString(),
				m.get("artist").getAsString(),
				m.get("playing").getAsBoolean(),
				m.get("position").getAsLong(),
				m.get("length").getAsLong(),
				m.get("art").isJsonNull() ? null : m.get("art").getAsString(),
				System.currentTimeMillis());
	}

	@Override
	public void tick(Minecraft mc) {
		if (!keys.on() || Mc.screen() != null) return;
		String[] actions = {"previous", "play_pause", "next"};
		int[] codes = {KP_4, KP_5, KP_6};
		for (int i = 0; i < 3; i++) {
			boolean down = Mc.keyDown(codes[i]);
			if (down && !held[i]) PRESENCE.info("{\"media\":\"" + actions[i] + "\"}");
			held[i] = down;
		}
	}

	@Override
	public boolean visible(Minecraft mc) {
		return track != null;
	}

	@Override
	public int width(Minecraft mc) {
		return W;
	}

	@Override
	public int height(Minecraft mc) {
		return H;
	}

	private Identifier art(Minecraft mc, String path) {
		if (path == null || !path.equals(artPath)) {
			if (artId != null) mc.getTextureManager().release(artId);
			artId = null;
			artPath = path;
			artLoading = path == null ? null : CompletableFuture.supplyAsync(() -> {
				try {
					return ImageIO.read(new File(path));
				} catch (Exception e) {
					return null;
				}
			});
		}
		if (artId == null && artLoading != null && artLoading.isDone()) {
			BufferedImage img = artLoading.getNow(null);
			artLoading = null;
			if (img == null) return null;
			NativeImage n = new NativeImage(ART, ART, false);
			for (int y = 0; y < ART; y++) {
				for (int x = 0; x < ART; x++) Ver.setPixel(n, x, y, img.getRGB(x * img.getWidth() / ART, y * img.getHeight() / ART));
			}
			artId = Identifier.fromNamespaceAndPath("kromvia", "music_cover/" + System.nanoTime());
			mc.getTextureManager().register(artId, Chat.texture("kromvia music cover", n));
		}
		return artId;
	}

	private static String fit(Minecraft mc, String s, int w) {
		if (mc.font.width(s) <= w) return s;
		while (!s.isEmpty() && mc.font.width(s + "…") > w) s = s.substring(0, s.length() - 1);
		return s + "…";
	}

	private static String time(long ms) {
		long s = ms / 1000;
		return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
	}

	@Override
	public void render(Gfx g, Minecraft mc, int x, int y) {
		Track t = track;
		if (t == null) return;
		g.fill(x, y, x + W, y + H, 0x90000000);
		int tx = x + 3;
		if (cover.on()) {
			Identifier id = art(mc, t.art);
			if (id != null) g.blit(id, x + 3, y + 3, 0, 0, ART, ART, ART, ART, ART, ART);
			else g.fill(x + 3, y + 3, x + 3 + ART, y + 3 + ART, 0xFF1F1F23);
			tx += ART + 4;
		}
		int tw = x + W - 3 - tx;
		g.drawString(mc.font, fit(mc, (t.playing ? "" : "|| ") + t.title, tw), tx, y + 4, 0xFFFFFFFF, true);
		g.drawString(mc.font, fit(mc, t.artist, tw), tx, y + 14, 0xFFAAAAAA, true);
		if (bar.on() && t.length > 0) {
			long pos = Math.min(t.length, t.position + (t.playing ? System.currentTimeMillis() - t.at : 0));
			String left = time(pos);
			g.drawString(mc.font, left, tx, y + H - 11, 0xFF888888, false);
			int bx = tx + mc.font.width(left) + 3, bw = x + W - 3 - bx, by = y + H - 7;
			g.fill(bx, by, bx + bw, by + 2, 0xFF2C2C31);
			g.fill(bx, by, bx + (int) (bw * pos / t.length), by + 2, accent.argb());
		}
	}
}
