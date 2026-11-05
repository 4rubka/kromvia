package com.kromvia.client.fx;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.KScreen;
import com.kromvia.client.compat.Mc;
import com.kromvia.client.ui.Icons;
import com.kromvia.client.ui.UiStyle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

final class MusicScreen extends KScreen {
	private static final int ART = 96, W = 320, H = 180;
	private final long opened = System.currentTimeMillis();
	private final List<Hit> hits = new ArrayList<>();
	private int px, py;
	private boolean dragging;
	private float dragAt;

	private record Hit(int x, int y, int w, int h, Runnable run) {
		boolean over(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	MusicScreen() {
		super(Component.literal("Music"));
	}

	@Override
	protected void init() {
		px = (width - W) / 2;
		py = (height - H) / 2;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private int barX() {
		return px + ART + 24;
	}

	private int barW() {
		return W - ART - 36;
	}

	private int barY() {
		return py + 74;
	}

	@Override
	protected void drawBackground(Gfx g, int mouseX, int mouseY, float delta) {
		UiStyle.dim(g, width, height, UiStyle.since(opened, 200));
	}

	@Override
	protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
		hits.clear();
		float in = UiStyle.since(opened, 240);
		g.pose().pushMatrix();
		g.pose().translate(0, (1 - in) * 12);
		UiStyle.panel(g, px, py, W, H);
		Media.Track t = Media.INSTANCE.track;
		if (t == null) {
			Icons.draw(g, "music", px + W / 2 - 16, py + 46, 32, UiStyle.MUTED);
			g.drawCenteredString(font, "Nothing is playing", px + W / 2, py + 90, UiStyle.TEXT);
			g.drawCenteredString(font, "Play something in Spotify, a browser or any media player.", px + W / 2, py + 104, UiStyle.MUTED);
			g.pose().popMatrix();
			return;
		}

		int ax = px + 12, ay = py + 12;
		int pixels = Cover.pixels(minecraft, ART);
		Identifier art = Media.INSTANCE.art.get(minecraft, t.art(), pixels);
		if (art != null) {
			g.blit(art, ax, ay, 0, 0, ART, ART, pixels, pixels, pixels, pixels);
		} else {
			g.fill(ax, ay, ax + ART, ay + ART, 0xFF14171D);
			Icons.draw(g, "music", ax + ART / 2 - 16, ay + ART / 2 - 16, 32, UiStyle.LINE);
		}

		int tx = barX(), tw = barW();
		g.drawString(font, fit(t.title(), tw), tx, py + 16, UiStyle.TEXT, false);
		g.drawString(font, fit(t.artist(), tw), tx, py + 30, UiStyle.MUTED, false);
		if (t.album() != null && !t.album().isEmpty()) g.drawString(font, fit(t.album(), tw), tx, py + 42, 0xFF6B7280, false);
		if (t.player() != null) {
			Icons.draw(g, "speaker", tx, py + 55, 10, UiStyle.SIDE);
			g.drawString(font, fit(t.player(), tw - 14), tx + 14, py + 56, UiStyle.SIDE, false);
		}

		long pos = t.now();
		int bx = barX(), bw = barW(), by = barY();
		boolean barHover = mouseX >= bx && mouseX < bx + bw && mouseY >= by - 4 && mouseY < by + 6;
		if (dragging) dragAt = Mth.clamp((mouseX - bx) / (float) bw, 0, 1);
		float f = t.length() > 0 ? (dragging ? dragAt : pos / (float) t.length()) : 0;
		g.fill(bx, by, bx + bw, by + 3, 0xFF23272F);
		g.fill(bx, by, bx + Math.round(bw * f), by + 3, UiStyle.FRONT);
		if (barHover || dragging) {
			int kx = bx + Math.round(bw * f);
			g.fill(kx - 3, by - 2, kx + 3, by + 5, UiStyle.TOP);
		}
		if (t.length() > 0) {
			long shown = dragging ? (long) (dragAt * t.length()) : pos;
			g.drawString(font, time(shown), bx, by + 8, UiStyle.MUTED, false);
			String end = time(t.length());
			g.drawString(font, end, bx + bw - font.width(end), by + 8, UiStyle.MUTED, false);
			hits.add(new Hit(bx, by - 4, bw, 10, () -> {
				dragging = true;
				dragAt = Mth.clamp((mouseX - bx) / (float) bw, 0, 1);
			}));
		}

		int cy = py + 100, mid = bx + bw / 2;
		boolean shuffleOn = Boolean.TRUE.equals(t.shuffle());
		String repeat = t.repeat();
		button(g, mouseX, mouseY, mid - 74, cy + 4, 16, "shuffle", t.shuffle() == null ? -1 : shuffleOn ? 1 : 0,
				() -> Media.local(t.with(t.playing(), t.now(), !shuffleOn, repeat)), () -> Media.send("shuffle", !shuffleOn));
		button(g, mouseX, mouseY, mid - 42, cy + 4, 16, "previous", 0, null, () -> Media.send("previous", null));
		boolean overPlay = mouseX >= mid - 14 && mouseX < mid + 14 && mouseY >= cy - 4 && mouseY < cy + 24;
		circle(g, mid, cy + 10, 14, overPlay ? UiStyle.TOP : UiStyle.TEXT);
		Icons.draw(g, t.playing() ? "pause" : "play", mid - 8, cy + 2, 16, 0xFF0B0D11);
		hits.add(new Hit(mid - 14, cy - 4, 28, 28, () -> {
			Media.local(t.with(!t.playing(), t.now(), t.shuffle(), repeat));
			Media.send("play_pause", null);
		}));
		button(g, mouseX, mouseY, mid + 26, cy + 4, 16, "next", 0, null, () -> Media.send("next", null));
		String nextRepeat = "none".equals(repeat) ? "playlist" : "playlist".equals(repeat) ? "track" : "none";
		button(g, mouseX, mouseY, mid + 58, cy + 4, 16, "track".equals(repeat) ? "repeat_one" : "repeat",
				repeat == null ? -1 : "none".equals(repeat) ? 0 : 1,
				() -> Media.local(t.with(t.playing(), t.now(), t.shuffle(), nextRepeat)), () -> Media.send("repeat", nextRepeat));

		int ly = py + ART + 26;
		g.fill(px + 12, ly - 6, px + W - 12, ly - 5, UiStyle.LINE);
		g.drawString(font, "Players", px + 12, ly, UiStyle.MUTED, false);
		int cx = px + 12, chipY = ly + 12;
		for (Media.Source s : t.players()) {
			String label = s.name() == null || s.name().isEmpty() ? "Player" : s.name();
			int cw = font.width(label) + 22;
			if (cx + cw > px + W - 12) break;
			boolean current = s.id() != null && s.id().equals(t.playerId());
			boolean over = mouseX >= cx && mouseX < cx + cw && mouseY >= chipY && mouseY < chipY + 16;
			g.fill(cx, chipY, cx + cw, chipY + 16, current ? 0xFF1C1633 : over ? 0xFF161B22 : 0xFF0F1216);
			g.fill(cx, chipY + 15, cx + cw, chipY + 16, current ? UiStyle.FRONT : UiStyle.LINE);
			g.fill(cx + 6, chipY + 6, cx + 10, chipY + 10, s.playing() ? 0xFF4ADE80 : 0xFF4B5563);
			g.drawString(font, label, cx + 14, chipY + 4, current ? UiStyle.TEXT : UiStyle.MUTED, false);
			String id = s.id();
			if (!current && id != null) hits.add(new Hit(cx, chipY, cw, 16, () -> Media.send("select", id)));
			cx += cw + 4;
		}
		if (t.players().isEmpty()) g.drawString(font, t.player() == null ? "" : t.player(), px + 12, chipY + 4, UiStyle.TEXT, false);
		g.pose().popMatrix();
	}

	private void button(Gfx g, int mouseX, int mouseY, int x, int y, int size, String icon, int state, Runnable local, Runnable send) {
		boolean over = state >= 0 && mouseX >= x - 4 && mouseX < x + size + 4 && mouseY >= y - 4 && mouseY < y + size + 4;
		int color = state < 0 ? 0xFF3A3F49 : state == 1 ? UiStyle.FRONT : over ? UiStyle.TEXT : UiStyle.MUTED;
		Icons.draw(g, icon, x, y, size, color);
		if (state == 1) g.fill(x + size / 2 - 1, y + size + 3, x + size / 2 + 1, y + size + 5, UiStyle.FRONT);
		if (state >= 0) hits.add(new Hit(x - 4, y - 4, size + 8, size + 8, () -> {
			if (local != null) local.run();
			send.run();
		}));
	}

	private static void circle(Gfx g, int cx, int cy, int r, int color) {
		for (int dy = -r; dy < r; dy++) {
			double y = dy + 0.5;
			int half = (int) Math.round(Math.sqrt(r * r - y * y));
			g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
		}
	}

	private String fit(String s, int w) {
		if (s == null) return "";
		if (font.width(s) <= w) return s;
		while (!s.isEmpty() && font.width(s + "…") > w) s = s.substring(0, s.length() - 1);
		return s + "…";
	}

	private static String time(long ms) {
		long s = ms / 1000;
		return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
	}

	@Override
	protected void mouseDown(double x, double y) {
		for (int i = hits.size() - 1; i >= 0; i--) {
			if (hits.get(i).over(x, y)) {
				hits.get(i).run().run();
				return;
			}
		}
	}

	@Override
	protected void mouseUp() {
		if (!dragging) return;
		dragging = false;
		Media.Track t = Media.INSTANCE.track;
		if (t == null || t.length() <= 0) return;
		long to = (long) (dragAt * t.length());
		Media.local(t.with(t.playing(), to, t.shuffle(), t.repeat()));
		Media.send("seek", to);
	}

	@Override
	public void onClose() {
		Mc.setScreen(null);
	}
}
