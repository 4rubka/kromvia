package com.kromvia.client.fx;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.Mc;
import com.kromvia.client.module.Module;
import com.kromvia.client.module.setting.BoolSetting;
import com.kromvia.client.module.setting.ChoiceSetting;
import com.kromvia.client.module.setting.ColorSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Media extends Module {
	private static final Logger PRESENCE = LoggerFactory.getLogger("kromvia-presence");
	private static final int KP_4 = 324, KP_5 = 325, KP_6 = 326;
	private static final int ART = 32, W = 150, H = ART + 6;

	record Source(String id, String name, boolean playing) {}

	record Track(String title, String artist, boolean playing, long position, long length, String art, long at,
			String album, String player, String playerId, Boolean shuffle, String repeat, List<Source> players) {
		long now() {
			long pos = position + (playing ? System.currentTimeMillis() - at : 0);
			return length > 0 ? Math.min(length, pos) : pos;
		}

		Track with(boolean playing, long position, Boolean shuffle, String repeat) {
			return new Track(title, artist, playing, position, length, art, System.currentTimeMillis(), album, player, playerId, shuffle, repeat, players);
		}
	}

	private static final int[] MENU_KEYS = {77, 75, 85, 89, 320};

	private final BoolSetting cover = add(new BoolSetting("cover", "Cover art", true));
	private final BoolSetting bar = add(new BoolSetting("bar", "Progress bar", true));
	private final BoolSetting keys = add(new BoolSetting("keys", "Numpad 4 / 5 / 6: previous, pause, next", true));
	private final ColorSetting accent = add(new ColorSetting("accent", "Bar colour", 0xFF8B5CF6));
	private final ChoiceSetting menu = add(new ChoiceSetting("menu", "Music menu key", 0, "M", "K", "U", "Y", "Numpad 0"));
	private boolean menuHeld;

	volatile Track track;
	final Cover art = new Cover();
	private final boolean[] held = new boolean[3];

	Media() {
		super("music", "Music", Category.HUD, Corner.TOP_RIGHT, false);
		icon(Items.MUSIC_DISC_CAT);
	}

	static final Media INSTANCE = new Media();

	static Track current() {
		return INSTANCE.enabled || Lyrics.INSTANCE.enabled ? INSTANCE.track : null;
	}

	private static String text(JsonObject m, String key) {
		JsonElement e = m.get(key);
		return e == null || e.isJsonNull() ? null : e.getAsString();
	}

	static void set(JsonObject m) {
		if (m == null) {
			INSTANCE.track = null;
			return;
		}
		List<Source> players = new ArrayList<>();
		if (m.has("players") && m.get("players").isJsonArray()) {
			for (JsonElement e : m.getAsJsonArray("players")) {
				JsonObject p = e.getAsJsonObject();
				players.add(new Source(text(p, "id"), text(p, "name"), p.has("playing") && p.get("playing").getAsBoolean()));
			}
		}
		JsonElement shuffle = m.get("shuffle");
		INSTANCE.track = new Track(
				m.get("title").getAsString(),
				m.get("artist").getAsString(),
				m.get("playing").getAsBoolean(),
				m.get("position").getAsLong(),
				m.get("length").getAsLong(),
				text(m, "art"),
				System.currentTimeMillis(),
				text(m, "album"),
				text(m, "player"),
				text(m, "playerId"),
				shuffle == null || shuffle.isJsonNull() ? null : shuffle.getAsBoolean(),
				text(m, "repeat"),
				players);
	}

	static void send(String action, Object value) {
		JsonObject o = new JsonObject();
		o.addProperty("media", action);
		if (value instanceof Number n) o.addProperty("value", n);
		else if (value instanceof Boolean b) o.addProperty("value", b);
		else if (value != null) o.addProperty("value", value.toString());
		PRESENCE.info(o.toString());
	}

	static void local(Track t) {
		INSTANCE.track = t;
	}

	@Override
	public void tick(Minecraft mc) {
		boolean open = Mc.screen() == null && Mc.keyDown(MENU_KEYS[menu.index()]);
		if (open && !menuHeld) Mc.setScreen(new MusicScreen());
		menuHeld = open;
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
			int px = Cover.pixels(mc, ART);
			Identifier id = art.get(mc, t.art, px);
			if (id != null) g.blit(id, x + 3, y + 3, 0, 0, ART, ART, px, px, px, px);
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
