package com.kromvia.client.fx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.Mc;
import com.kromvia.client.module.Module;
import com.kromvia.client.module.Freecam;
import com.kromvia.client.module.setting.BoolSetting;
import com.kromvia.client.module.setting.ChoiceSetting;
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
import net.minecraft.world.phys.Vec3;

public final class Lyrics extends Module {
	private static final Pattern TAG = Pattern.compile("^\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]");
	private static final long MOVE_MS = 420;

	record Line(long at, String text) {}

	private final SliderSetting size = add(new SliderSetting("size", "Size", 1.5F, 1, 3, 0.25F, "x"));
	private final SliderSetting height = add(new SliderSetting("position", "Height on screen", 18, 5, 85, 1, "%"));
	private final BoolSetting next = add(new BoolSetting("next", "Show the next line", true));
	private final SliderSetting offset = add(new SliderSetting("offset", "Timing", 0, -2000, 2000, 100, "ms"));
	private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFFFFFFF));
	private final ChoiceSetting display = add(new ChoiceSetting("display", "Display", 0, "Screen", "World: line", "World: words"));
	private final ChoiceSetting colorMode = add(new ChoiceSetting("color_mode", "Colour style", 0, "Solid", "Gradient", "Rainbow"));
	private final ColorSetting color2 = add(new ColorSetting("color2", "Second colour", 0xFF8B5CF6));
	private final BoolSetting shadow = add(new BoolSetting("shadow", "Text shadow", true));
	private final BoolSetting plate = add(new BoolSetting("plate", "Background", false));
	private final ChoiceSetting animation = add(new ChoiceSetting("animation", "Word animation", 0, "Pop", "Fade", "Rise", "Drop"));
	private final SliderSetting worldDistance = add(new SliderSetting("world_distance", "3D: distance", 6, 2, 16, 0.5F, " blocks"));
	private final SliderSetting worldHeight = add(new SliderSetting("world_height", "3D: height", 0.5F, -2, 3, 0.25F, " blocks"));
	private final SliderSetting worldSize = add(new SliderSetting("world_size", "3D: text size", 1.5F, 0.5F, 4, 0.25F, "x"));
	private final SliderSetting spread = add(new SliderSetting("spread", "3D words: spread", 2.5F, 0.5F, 6, 0.5F, " blocks"));
	private final SliderSetting wordLife = add(new SliderSetting("word_life", "3D words: stay for", 4, 1, 10, 0.5F, " s"));
	private final ChoiceSetting facing = add(new ChoiceSetting("facing", "3D: turn to camera", 0, "Always", "Only when it appears"));
	private final BoolSetting floatAway = add(new BoolSetting("float_away", "3D: float up when fading", true));
	private final BoolSetting throughWalls = add(new BoolSetting("through_walls", "3D: show through walls", false));

	record Floating(String text, Vec3 pos, float yaw, float pitch, long born, long life, float size, int color) {}

	private final List<Floating> floating = new ArrayList<>();
	private Vec3 lineAnchor;
	private float lineYaw, linePitch;
	private int worldLine = -2, worldWord = -1;
	private final java.util.Random random = new java.util.Random();

	private String key;
	private volatile List<Line> lines = List.of();
	private int shown = -1, prev = -1;
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

	private static final Pattern JUNK = Pattern.compile("\\s*[(\\[][^)\\]]*\\b(official|video|audio|lyrics?|visuali[sz]er|remaster(ed)?|live|version|edit|feat|ft|prod|explicit|clean|hd|hq|4k|mv|m/v)\\b[^)\\]]*[)\\]]", Pattern.CASE_INSENSITIVE);
	private static final Pattern FEAT = Pattern.compile("\\s+(feat\\.?|ft\\.?|featuring|prod\\.?)\\s.*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern DASH_TAIL = Pattern.compile("\\s+[-–—]\\s+(\\d{4}\\s+)?(remaster|live|radio edit|single|mono|stereo|from).*$", Pattern.CASE_INSENSITIVE);

	static String cleanTitle(String title) {
		String t = JUNK.matcher(title).replaceAll("");
		t = DASH_TAIL.matcher(t).replaceAll("");
		return FEAT.matcher(t).replaceAll("").replace("\"", "").trim();
	}

	static String cleanArtist(String artist) {
		String a = artist.replaceAll("(?i)\\s*-\\s*topic$", "").replaceAll("(?i)vevo$", "").trim();
		a = a.split("\\s*(,|&|;|\\bx\\b|\\bfeat\\.?|\\bft\\.?|\\band\\b)\\s*")[0].trim();
		return a.isEmpty() ? artist.trim() : a;
	}

	private static List<Line> fetch(String artist, String title, long length) {
		String a = cleanArtist(artist), t = cleanTitle(title);
		List<String> queries = new ArrayList<>();
		int dash = t.indexOf(" - ");
		if (dash > 0) {
			String left = t.substring(0, dash).trim(), right = t.substring(dash + 3).trim();
			queries.add("track_name=" + enc(right) + "&artist_name=" + enc(cleanArtist(left)));
			queries.add("q=" + enc(left + " " + right));
		}
		queries.add("track_name=" + enc(t) + "&artist_name=" + enc(a));
		if (!a.equals(artist.trim()) || !t.equals(title.trim())) queries.add("track_name=" + enc(title.trim()) + "&artist_name=" + enc(artist.trim()));
		queries.add("q=" + enc(a + " " + t));
		queries.add("track_name=" + enc(t));
		JsonObject best = null;
		double bestGap = Double.MAX_VALUE;
		for (String q : queries) {
			JsonObject[] hit = new JsonObject[1];
			double gap = search(q, length, hit);
			if (hit[0] != null && gap < bestGap) {
				best = hit[0];
				bestGap = gap;
			}
			if (bestGap <= 5) break;
		}
		return best == null ? List.of() : parse(best.get("syncedLyrics").getAsString());
	}

	private static String enc(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	private static double search(String q, long length, JsonObject[] out) {
		try {
			HttpURLConnection c = (HttpURLConnection) URI.create("https://lrclib.net/api/search?" + q).toURL().openConnection();
			c.setRequestProperty("User-Agent", "Kromvia (https://kromvia.com)");
			c.setConnectTimeout(5000);
			c.setReadTimeout(8000);
			if (c.getResponseCode() != 200) return Double.MAX_VALUE;
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
			out[0] = best;
			return bestGap;
		} catch (Exception e) {
			return Double.MAX_VALUE;
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
		if (t == null || ls.isEmpty() || Mc.hudHidden() || display.index() != 0) return;
		long pos = t.now() + (long) offset.value();
		int i = index(ls, pos);
		long now = System.currentTimeMillis();
		if (i != shown) {
			prev = shown;
			shown = i;
			shownAt = now;
		}
		float e = ease(Math.min(1, (now - shownAt) / (float) MOVE_MS));
		float big = size.value(), small = size.value() * 0.6F;
		int width = (int) (g.guiWidth() * 0.8F);
		float cx = g.guiWidth() / 2F, cy = g.guiHeight() * height.value() / 100;
		int main = color.argb(), dim = 0xFFAAAAAA;

		String old = text(ls, prev);
		float oldH = old == null ? 0 : height(mc, old, big, width);
		if (old != null && e < 1) {
			float sc = big * (1 - 0.2F * e);
			draw(g, mc, old, cx, cy - e * (oldH * 0.8F + 6), sc, width, alpha(main, 1 - e));
		}

		String cur = text(ls, i);
		float bottom = cy;
		if (cur != null) {
			boolean fromNext = next.on() && prev == i - 1;
			float startY = fromNext ? cy + (old == null ? 0 : oldH + 6) : cy + 10;
			float sc = fromNext ? small + (big - small) * e : big * (0.85F + 0.15F * e);
			int col = fromNext ? mix(dim, main, e) : main;
			float a = fromNext ? 0.7F + 0.3F * e : e;
			float y = startY + (cy - startY) * e;
			bottom = y + draw(g, mc, cur, cx, y, sc, width, alpha(col, a));
		} else if (i + 1 < ls.size() && ls.get(i + 1).at - pos > 2500) {
			float pulse = (float) (Math.sin(now / 260.0) * 0.5 + 0.5);
			g.pose().pushMatrix();
			g.pose().translate(cx, cy);
			g.pose().scale(big, big);
			g.drawCenteredString(mc.font, "• • •", 0, 0, alpha(main, (0.35F + 0.5F * pulse) * Math.max(e, 0.3F)));
			g.pose().popMatrix();
			bottom = cy + 11 * big;
		}

		String upcoming = text(ls, i + 1);
		if (next.on() && upcoming != null && cur != null) {
			List<String> first = wrap(mc, upcoming, (int) (width / small));
			g.pose().pushMatrix();
			g.pose().translate(cx, bottom + 6 + (1 - e) * 8);
			g.pose().scale(small, small);
			g.drawCenteredString(mc.font, first.get(0), 0, 0, alpha(dim, 0.7F * e));
			g.pose().popMatrix();
		}
	}

	private static String text(List<Line> ls, int i) {
		return i >= 0 && i < ls.size() && !ls.get(i).text.isEmpty() ? ls.get(i).text : null;
	}

	private static float backOut(float t) {
		float u = t - 1;
		return 1 + 2.7F * u * u * u + 1.7F * u * u;
	}

	private static float ease(float p) {
		return 1 - (1 - p) * (1 - p) * (1 - p);
	}

	private static int mix(int a, int b, float t) {
		int r = Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
		int gr = Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
		int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
		return 0xFF000000 | r << 16 | gr << 8 | bl;
	}

	private static float height(Minecraft mc, String text, float scale, int width) {
		return wrap(mc, text, (int) (width / scale)).size() * 11 * scale;
	}

	private float draw(Gfx g, Minecraft mc, String text, float x, float y, float scale, int width, int argb) {
		List<String> parts = wrap(mc, text, (int) (width / scale));
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		long now = System.currentTimeMillis();
		for (int k = 0; k < parts.size(); k++) {
			String part = parts.get(k);
			int w = mc.font.width(part);
			if (plate.on()) g.fill(-w / 2 - 3, k * 11 - 2, w - w / 2 + 3, k * 11 + 10, alpha(0x80000000, ((argb >>> 24) / 255F)));
			int cx = -w / 2;
			for (int c = 0; c < part.length(); c++) {
				String ch = String.valueOf(part.charAt(c));
				int col = tint(c, part.length(), now, argb);
				g.drawString(mc.font, ch, cx, k * 11, col, shadow.on());
				cx += mc.font.width(ch);
			}
		}
		g.pose().popMatrix();
		return parts.size() * 11 * scale;
	}

	private int tint(int i, int length, long now, int base) {
		int a = base >>> 24;
		if ((base & 0xFFFFFF) != (color.argb() & 0xFFFFFF)) return base;
		int c = switch (colorMode.index()) {
			case 1 -> mix(color.argb(), color2.argb(), length > 1 ? i / (float) (length - 1) : 0);
			case 2 -> 0xFF000000 | net.minecraft.util.Mth.hsvToRgb(((now / 20 + i * 12) % 360) / 360F, 0.6F, 1F);
			default -> color.argb();
		};
		return a << 24 | c & 0xFFFFFF;
	}

	private static boolean shouted(String word) {
		String letters = word.replaceAll("[^\\p{L}]", "");
		return word.endsWith("!") || letters.length() > 1 && letters.equals(letters.toUpperCase(java.util.Locale.ROOT)) && !letters.equals(letters.toLowerCase(java.util.Locale.ROOT));
	}

	public void world(Minecraft mc, com.mojang.blaze3d.vertex.PoseStack pose, Object out, Vec3 cam) {
		if (!enabled || display.index() == 0) {
			floating.clear();
			return;
		}
		Media.Track t = Media.current();
		List<Line> ls = lines;
		long now = System.currentTimeMillis();
		floating.removeIf(f -> now - f.born > f.life);
		if (t == null || ls.isEmpty() || mc.player == null) {
			worldLine = -2;
			drawFloating(mc, pose, out, cam, now);
			return;
		}
		long pos = t.now() + (long) offset.value();
		int i = index(ls, pos);
		float yaw = Freecam.active() ? Freecam.yaw() : mc.player.getViewYRot(Ver.partial(mc));
		float pitch = Freecam.active() ? Freecam.pitch() : mc.player.getViewXRot(Ver.partial(mc));
		if (i != worldLine) {
			worldLine = i;
			worldWord = -1;
			double ry = Math.toRadians(yaw), rp = Math.toRadians(pitch * 0.4F);
			Vec3 look = new Vec3(-Math.sin(ry) * Math.cos(rp), -Math.sin(rp), Math.cos(ry) * Math.cos(rp));
			lineAnchor = cam.add(look.scale(worldDistance.value())).add(0, worldHeight.value(), 0);
			lineYaw = yaw;
			linePitch = pitch * 0.4F;
			if (display.index() == 1) {
				floating.removeIf(f -> f.life == Long.MAX_VALUE && floating.stream().anyMatch(o -> o.life == Long.MAX_VALUE && o.born > f.born));
				floating.replaceAll(f -> f.life == Long.MAX_VALUE ? new Floating(f.text, f.pos, f.yaw, f.pitch, f.born, now - f.born + 600, f.size, f.color) : f);
			}
		}
		String text = i >= 0 && i < ls.size() ? ls.get(i).text : "";
		String[] words = text.isEmpty() ? new String[0] : text.split(" ");
		long start = i >= 0 ? ls.get(i).at : 0;
		long end = i + 1 < ls.size() ? ls.get(i + 1).at : start + 4000;
		long span = Math.max(300, Math.min(end - start, 400L * Math.max(1, text.length() / 4)) * 85 / 100);
		int total = Math.max(1, text.replace(" ", "").length());
		int done = 0, reached = -1;
		for (int w = 0; w < words.length; w++) {
			if (start + span * done / total <= pos) reached = w;
			done += words[w].length();
		}
		while (worldWord < reached) {
			worldWord++;
			spawn(words, worldWord, cam, yaw, now);
		}
		drawFloating(mc, pose, out, cam, now);
	}

	private void spawn(String[] words, int w, Vec3 cam, float yaw, long now) {
		String word = words[w];
		float big = 1 + (shouted(word) ? 0.3F : 0);
		int col = colorMode.index() == 1 ? mix(color.argb(), color2.argb(), words.length > 1 ? w / (float) (words.length - 1) : 0)
				: colorMode.index() == 2 ? 0xFF000000 | net.minecraft.util.Mth.hsvToRgb(random.nextFloat(), 0.6F, 1F) : color.argb();
		if (display.index() == 1) {
			floating.add(new Floating(String.join(" ", java.util.Arrays.copyOfRange(words, 0, w + 1)), lineAnchor, lineYaw, linePitch, now, Long.MAX_VALUE, big, col));
			return;
		}
		double ry = Math.toRadians(yaw);
		Vec3 right = new Vec3(-Math.cos(ry), 0, -Math.sin(ry));
		double side = (random.nextDouble() * 2 - 1) * spread.value(), up = (random.nextDouble() * 2 - 1) * spread.value() * 0.4;
		double depth = worldDistance.value() * (0.8 + random.nextDouble() * 0.4);
		Vec3 look = new Vec3(-Math.sin(ry), 0, Math.cos(ry));
		Vec3 at = cam.add(look.scale(depth)).add(right.scale(side)).add(0, worldHeight.value() + up, 0);
		floating.add(new Floating(word, at, yaw, 0, now, (long) (wordLife.value() * 1000), big, col));
	}

	private void drawFloating(Minecraft mc, com.mojang.blaze3d.vertex.PoseStack pose, Object out, Vec3 cam, long now) {
		boolean line = display.index() == 1;
		for (int k = 0; k < floating.size(); k++) {
			Floating f = floating.get(k);
			if (line && k < floating.size() - 1 && f.life == Long.MAX_VALUE) continue;
			float age = (now - f.born) / 1000F;
			float in = Math.min(1, age / 0.35F);
			float e = ease(in);
			float out_ = f.life == Long.MAX_VALUE ? 1 : Math.min(1, (f.life / 1000F - age) / 0.6F);
			float a = Math.max(0, Math.min(e, out_));
			if (animation.index() == 0) a = Math.min(1, age / 0.12F) * Math.max(0, out_);
			float sc = f.size;
			double dy = 0;
			switch (animation.index()) {
				case 0 -> sc *= backOut(in);
				case 2 -> dy = -(1 - e) * 0.4;
				case 3 -> dy = (1 - e) * 0.5;
				default -> {}
			}
			if (floatAway.on() && f.life != Long.MAX_VALUE && out_ < 1) dy += (1 - out_) * 0.6;
			Vec3 p = f.pos.add(0, dy, 0);
			float yaw, pitch;
			if (facing.index() == 0) {
				Vec3 d = p.subtract(cam);
				yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
				pitch = (float) Math.toDegrees(Math.atan2(-d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
			} else {
				yaw = f.yaw;
				pitch = f.pitch;
			}
			float s = 0.025F * worldSize.value() * sc;
			pose.pushPose();
			pose.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
			Types.mulPose(pose, new org.joml.Matrix4f().rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(pitch), 0));
			pose.scale(-s, -s, s);
			String text = f.text;
			float x = -mc.font.width(text) / 2F;
			int argb = alpha(f.color, a);
			int bg = plate.on() ? alpha(0x60000000, a) : 0;
			if (line) {
				int space = text.lastIndexOf(' ');
				String head = space < 0 ? "" : text.substring(0, space + 1), tail = space < 0 ? text : text.substring(space + 1);
				if (!head.isEmpty()) Types.text(out, pose, head, x, 0, alpha(f.color, 1), shadow.on(), throughWalls.on(), bg);
				float hx = x + mc.font.width(head), tw = mc.font.width(tail);
				pose.pushPose();
				pose.translate(hx + tw / 2, 4, 0);
				float pop = animation.index() == 0 ? 0.3F + 0.7F * e : 1;
				pose.scale(pop, pop, 1);
				pose.translate(-tw / 2, animation.index() == 2 ? (1 - e) * 8 : animation.index() == 3 ? -(1 - e) * 8 : 0, 0);
				Types.text(out, pose, tail, 0, -4, argb, shadow.on(), throughWalls.on(), bg);
				pose.popPose();
			} else {
				Types.text(out, pose, text, x, -4, argb, shadow.on(), throughWalls.on(), bg);
			}
			pose.popPose();
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
