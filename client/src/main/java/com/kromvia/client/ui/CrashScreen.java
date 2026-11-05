package com.kromvia.client.ui;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.KScreen;
import com.kromvia.client.compat.Mc;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class CrashScreen extends KScreen {
	private final Throwable error;
	private final String trace;

	public CrashScreen(Throwable error) {
		super(Component.literal("Crash"));
		this.error = error;
		StringWriter out = new StringWriter();
		error.printStackTrace(new PrintWriter(out));
		this.trace = out.toString();
	}

	@Override
	protected void init() {
		int w = 150;
		int y = height - 40;
		addRenderableWidget(new KButton(width / 2 - w - 4, y, w, 20, Component.literal("Back to the title screen"), b -> Mc.setScreen(null)));
		addRenderableWidget(new KButton(width / 2 + 4, y, w, 20, Component.literal("Copy the error"), b -> Minecraft.getInstance().keyboardHandler.setClipboard(trace)));
	}

	@Override
	protected void drawBackground(Gfx g, int mouseX, int mouseY, float delta) {
		UiStyle.dim(g, width, height, 1);
	}

	@Override
	protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
		super.draw(g, mouseX, mouseY, delta);
		int x = width / 2 - Math.min(width - 20, 420) / 2;
		int w = Math.min(width - 20, 420);
		g.drawString(font, "THE GAME CRASHED", x, 30, UiStyle.FRONT, false);
		g.drawString(font, "Kromvia caught it, so the game stayed open. Your world was left without saving the last moment.", x, 46, UiStyle.MUTED, false);
		int y = 70;
		for (String line : lines(String.valueOf(error), w)) {
			g.drawString(font, line, x, y, UiStyle.TEXT, false);
			y += font.lineHeight + 2;
		}
		y += 6;
		for (String line : trace.split("\n")) {
			if (y > height - 56) break;
			if (!line.trim().startsWith("at ")) continue;
			g.drawString(font, fit(line.trim(), w), x, y, UiStyle.MUTED, false);
			y += font.lineHeight + 1;
		}
	}

	private String fit(String s, int w) {
		if (font.width(s) <= w) return s;
		while (s.length() > 1 && font.width(s + "…") > w) s = s.substring(0, s.length() - 1);
		return s + "…";
	}

	private List<String> lines(String s, int w) {
		List<String> out = new ArrayList<>();
		while (!s.isEmpty() && out.size() < 4) {
			int n = s.length();
			while (n > 1 && font.width(s.substring(0, n)) > w) n--;
			out.add(s.substring(0, n));
			s = s.substring(n);
		}
		return out;
	}
}
