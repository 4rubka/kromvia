package com.kromvia.client.fx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.Mc;
import com.kromvia.client.module.Module;
import com.kromvia.client.module.setting.BoolSetting;
import com.kromvia.client.module.setting.ColorSetting;
import com.kromvia.client.module.setting.SliderSetting;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

public final class Lyrics extends Module {
	private static final Pattern TAG = Pattern.compile("^\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]");
	private static final long POP_MS = 300;

	record Line(long at, String text) {}

	private final SliderSetting size = add(new SliderSetting("size", "Size", 1.5F, 1, 3, 0.25F, "x"));
	private final SliderSetting height = add(new SliderSetting("height", "Height on screen", 30, 10, 85, 5, "%"));
	private final BoolSetting next = add(new BoolSetting("next", "Show the next line", true));
	private final SliderSetting offset = add(new SliderSetting("offset", "Timing", 0, -2000, 2000, 100, "ms"));
	private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFFFFFFF));

	private String key;
	private volatile List<Line> lines = List.of();
	private int shown = -1;
	private long shownAt;

	private Lyrics() {
		super("lyrics", "Lyrics", Category.HUD, Corner.TOP_LEFT, false);
		icon(Items.JUKEBOX);
	}

	static final Lyrics INSTANCE = new Lyrics();

	@Override
	public boolean hud() {
		return false;
	}

	@Override
	public void tick(Minecraft mc) {
		Media.Track t = Media.current();
		String k = t == null ? null : t.artist() + "\n" + t.title();
		if (Objects.equals(k, key)) return;
		key = k;
		lines = List.of();
		shown = -1;
		if (k != null) CompletableFuture.runAsync(() -> {
			List<Line> found = fetch(t.artist(), t.title(), t.length());
			if (k.equals(key)) lines = found;
		});
	}

	private static List<Line> fetch(String artist, String title, long length) {
		try {
			String q = "track_name=" + URLEncoder.encode(title, StandardCharsets.UTF_8) + "&artist_name=" + URLEncoder.encode(artist, StandardCharsets.UTF_8);
			HttpURLConnection c = (HttpURLConnection) URI.create("https://lrclib.net/api/search?" + q).toURL().openConnection();
			c.setRequestProperty("User-Agent", "Kromvia (https://kromvia.com)");
			c.setConnectTimeout(5000);
			c.setReadTimeout(8000);
			if (c.getResponseCode() != 200) return List.of();
			JsonArray results;
			try (InputStreamReader r = new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8)) {
				results = new Gson().fromJson(r, JsonArray.class);
			}
			JsonObject best = null;
			double bestGap = Double.MAX_VALUE;
			for (JsonElement e : results) {
				JsonObject o = e.getAsJsonObject();
				if (!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) continue;
				double gap = length > 0 && o.has("duration") && !o.get("duration").isJsonNull() ? Math.abs(o.get("duration").getAsDouble() - length / 1000.0) : 0;
				if (gap < bestGap) {
					best = o;
					bestGap = gap;
				}
			}
			return best == null ? List.of() : parse(best.get("syncedLyrics").getAsString());
		} catch (Exception e) {
			return List.of();
		}
	}

	static List<Line> parse(String lrc) {
		List<Line> out = new ArrayList<>();
		for (String raw : lrc.split("\\r?\\n")) {
			String rest = raw.trim();
			List<Long> times = new ArrayList<>();
			Matcher m;
			while ((m = TAG.matcher(rest)).find()) {
				times.add(Long.parseLong(m.group(1)) * 60_000 + Math.round(Double.parseDouble(m.group(2)) * 1000));
				rest = rest.substring(m.end()).trim();
			}
			for (long t : times) out.add(new Line(t, rest));
		}
		out.sort((a, b) -> Long.compare(a.at, b.at));
		return out;
	}

	static int index(List<Line> lines, long pos) {
		int i = -1;
		while (i + 1 < lines.size() && lines.get(i + 1).at <= pos) i++;
		return i;
	}

	@Override
	public void overlay(Gfx g, Minecraft mc) {
		Media.Track t = Media.current();
		List<Line> ls = lines;
		if (t == null || ls.isEmpty() || Mc.hudHidden()) return;
		int i = index(ls, t.now() + (long) offset.value());
		long now = System.currentTimeMillis();
		if (i != shown) {
			shown = i;
			shownAt = now;
		}
		if (i < 0 || ls.get(i).text.isEmpty()) return;
		float p = Math.min(1, (now - shownAt) / (float) POP_MS);
		float ease = 1 - (1 - p) * (1 - p) * (1 - p);
		float s = size.value() * (0.6F + 0.4F * ease);
		int cx = g.guiWidth() / 2, cy = Math.round(g.guiHeight() * height.value() / 100);
		g.pose().pushMatrix();
		g.pose().translate(cx, cy + (1 - ease) * 12);
		g.pose().scale(s, s);
		int y = 0;
		for (String part : wrap(mc, ls.get(i).text, (int) (g.guiWidth() * 0.8F / s))) {
			g.drawCenteredString(mc.font, part, 0, y, alpha(color.argb(), ease));
			y += 11;
		}
		g.pose().popMatrix();
		if (next.on() && i + 1 < ls.size() && !ls.get(i + 1).text.isEmpty()) {
			float ns = size.value() * 0.6F;
			g.pose().pushMatrix();
			g.pose().translate(cx, cy + y * s + 6);
			g.pose().scale(ns, ns);
			g.drawCenteredString(mc.font, wrap(mc, ls.get(i + 1).text, (int) (g.guiWidth() * 0.8F / ns)).get(0), 0, 0, alpha(0xFFAAAAAA, ease * 0.7F));
			g.pose().popMatrix();
		}
	}

	private static int alpha(int argb, float a) {
		return Math.max(8, Math.round((argb >>> 24) * a)) << 24 | argb & 0xFFFFFF;
	}

	private static List<String> wrap(Minecraft mc, String text, int width) {
		List<String> out = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			String tryLine = line.length() == 0 ? word : line + " " + word;
			if (line.length() > 0 && mc.font.width(tryLine) > width) {
				out.add(line.toString());
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(tryLine);
			}
		}
		out.add(line.toString());
		return out;
	}
}
