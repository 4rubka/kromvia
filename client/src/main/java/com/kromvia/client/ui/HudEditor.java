package com.kromvia.client.ui;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.KScreen;
import com.kromvia.client.compat.Mc;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;

import com.kromvia.client.Config;
import com.kromvia.client.module.Hud;
import com.kromvia.client.module.Module;
import com.kromvia.client.module.setting.Setting;
import com.kromvia.client.module.setting.SliderSetting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HudEditor extends KScreen {
	private static final int GRID = 4;
	private static final int SNAP = 6;
	private static final int EDGE = 4;

	private final Screen parent;
	private List<Hud.Box> boxes;
	private Module dragging;
	private double cursorX, cursorY, grabX, grabY;
	private boolean centredX, centredY;
	private int mouseX, mouseY;

	public HudEditor(Screen parent) {
		super(Component.literal("HUD editor"));
		this.parent = parent;
	}

	private Hud.Box at(double x, double y) {
		for (int i = boxes.size() - 1; i >= 0; i--) {
			Hud.Box b = boxes.get(i);
			if (x >= b.x && y >= b.y && x < b.x + b.w && y < b.y + b.h) return b;
		}
		return null;
	}

	private Hud.Box box(Module m) {
		for (Hud.Box b : boxes) if (b.module == m) return b;
		return null;
	}

	private static SliderSetting size(Module m) {
		for (Setting<?> s : m.settings) if (s instanceof SliderSetting && s.id.equals("scale")) return (SliderSetting) s;
		return null;
	}

	@Override
	protected void drawBackground(Gfx g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, width, height, 0x66000000);
	}

	@Override
	protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		boxes = Hud.layout(minecraft, width, height, true);
		if (dragging != null && centredX) g.fill(width / 2, 0, width / 2 + 1, height, 0x998B5CF6);
		if (dragging != null && centredY) g.fill(0, height / 2, width, height / 2 + 1, 0x998B5CF6);
		Hud.Box hover = dragging != null ? box(dragging) : at(mouseX, mouseY);
		for (Hud.Box b : boxes) {
			boolean hot = b == hover;
			g.fill(b.x - 1, b.y - 1, b.x + b.w + 1, b.y + b.h + 1, hot ? 0x448B5CF6 : 0x22FFFFFF);
			int edge = hot ? UiStyle.FRONT : 0x66FFFFFF;
			g.fill(b.x - 1, b.y - 1, b.x + b.w + 1, b.y, edge);
			g.fill(b.x - 1, b.y + b.h, b.x + b.w + 1, b.y + b.h + 1, edge);
			g.fill(b.x - 1, b.y, b.x, b.y + b.h, edge);
			g.fill(b.x + b.w, b.y, b.x + b.w + 1, b.y + b.h, edge);
			if (b.live) {
				Hud.drawScaled(g, minecraft, b.module, b.x, b.y);
			} else {
				g.pose().pushMatrix();
				g.pose().translate(b.x, b.y);
				g.pose().scale(b.module.scale(), b.module.scale());
				g.drawString(font, b.module.sample(), 3, 2, UiStyle.MUTED);
				g.pose().popMatrix();
			}
		}
		if (hover != null) {
			SliderSetting s = size(hover.module);
			String tip = hover.module.name + (s != null ? "  " + Math.round(s.value()) + "%" : "");
			int ty = hover.y > 14 ? hover.y - 12 : hover.y + hover.h + 3;
			g.drawString(font, tip, Math.min(hover.x, width - font.width(tip) - 2), ty, UiStyle.TEXT);
		}
		g.drawCenteredString(font, "Drag to move  ·  Scroll to resize  ·  R to reset  ·  Esc to finish", width / 2, height - 14, UiStyle.MUTED);
		super.draw(g, mouseX, mouseY, delta);
	}

	@Override
	protected void mouseDown(double x, double y) {
		Hud.Box b = boxes == null ? null : at(x, y);
		if (b == null) return;
		dragging = b.module;
		cursorX = x;
		cursorY = y;
		grabX = x - b.x;
		grabY = y - b.y;
	}

	@Override
	protected boolean mouseDrag(double dx, double dy) {
		if (dragging == null) return false;
		Hud.Box b = box(dragging);
		if (b == null) return true;
		cursorX += dx;
		cursorY += dy;
		int freeX = Math.max(1, width - b.w), freeY = Math.max(1, height - b.h);
		int nx = snap((int) Math.round(cursorX - grabX), freeX);
		int ny = snap((int) Math.round(cursorY - grabY), freeY);
		centredX = nx == freeX / 2;
		centredY = ny == freeY / 2;
		dragging.hudX = nx / (float) freeX;
		dragging.hudY = ny / (float) freeY;
		return true;
	}

	private static int snap(int v, int free) {
		int[] targets = {EDGE, free / 2, free - EDGE};
		for (int t : targets) if (Math.abs(v - t) <= SNAP) return t;
		v = Math.round(v / (float) GRID) * GRID;
		return Math.max(0, Math.min(free, v));
	}

	@Override
	protected void mouseUp() {
		if (dragging != null) Config.save();
		dragging = null;
		centredX = centredY = false;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		Hud.Box b = boxes == null ? null : at(x, y);
		SliderSetting s = b == null ? null : size(b.module);
		if (s == null) return false;
		s.setFraction(s.fraction() + Math.signum(scrollY) * s.step / (s.max - s.min));
		Config.save();
		return true;
	}

	@Override
	protected boolean keyDown(int key) {
		if (key != InputConstants.KEY_R || boxes == null) return false;
		Hud.Box b = at(mouseX, mouseY);
		if (b == null) return false;
		b.module.hudX = b.module.hudY = -1;
		Config.save();
		return true;
	}

	@Override
	public void onClose() {
		Config.save();
		Mc.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
