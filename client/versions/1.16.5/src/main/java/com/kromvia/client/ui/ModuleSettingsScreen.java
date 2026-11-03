package com.kromvia.client.ui;

import com.kromvia.client.compat.Mc;
import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.KScreen;
import java.util.ArrayList;
import java.util.List;

import com.kromvia.client.Config;
import com.kromvia.client.module.Features;
import com.kromvia.client.module.Hud;
import com.kromvia.client.module.Module;
import com.kromvia.client.module.setting.BoolSetting;
import com.kromvia.client.module.setting.ChoiceSetting;
import com.kromvia.client.module.setting.ColorSetting;
import com.kromvia.client.module.setting.Setting;
import com.kromvia.client.module.setting.SliderSetting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModuleSettingsScreen extends KScreen {
	private static final int ROW = 24;
	private final Screen parent;
	private final Module module;
	private final List<Placed> placed = new ArrayList<>();
	private final List<String[]> labels = new ArrayList<>();
	private final long opened = System.currentTimeMillis();
	private int px, py, pw, ph, scroll, contentH;

	private static final class Placed {
		private final AbstractWidget widget;
		private final int y;
	
		Placed(AbstractWidget widget, int y) {
			this.widget = widget;
			this.y = y;
		}
	
		AbstractWidget widget() {
			return widget;
		}
	
		int y() {
			return y;
		}
	}

	public ModuleSettingsScreen(Screen parent, Module module) {
		super(new net.minecraft.network.chat.TextComponent(module.name));
		this.parent = parent;
		this.module = module;
	}

	private int listX() {
		return px + pw / 2 + 6;
	}

	private int listW() {
		return pw / 2 - 16;
	}

	@Override
	protected void init() {
		placed.clear();
		labels.clear();
		pw = Math.min(width - 40, 460);
		ph = Math.min(height - 30, 320);
		px = (width - pw) / 2;
		py = (height - ph) / 2;

		addButton(new KButton(px + 10, py + ph - 28, pw / 2 - 26, 20, new net.minecraft.network.chat.TextComponent("Back"), b -> onClose()));
		addButton(new KButton(px + pw - 90, py + 8, 80, 18, new net.minecraft.network.chat.TextComponent(module.enabled ? "On" : "Off"), b -> {
			module.enabled = !module.enabled;
			if (!module.enabled) module.disable(minecraft);
			b.setMessage(new net.minecraft.network.chat.TextComponent(module.enabled ? "On" : "Off"));
			Config.save();
		}, () -> module.enabled));

		int x = listX(), w = listW();
		int y = 0;
		for (Setting<?> s : module.settings) {
			if (!s.shown) continue;
			if (s instanceof BoolSetting) {
				BoolSetting b = (BoolSetting) s;
				place(new KButton(x, 0, w, 20, new net.minecraft.network.chat.TextComponent(b.name + ": " + (b.on() ? "On" : "Off")), btn -> {
					b.set(!b.on());
					btn.setMessage(new net.minecraft.network.chat.TextComponent(b.name + ": " + (b.on() ? "On" : "Off")));
					Config.save();
				}, b::on), y);
			} else if (s instanceof ChoiceSetting) {
				ChoiceSetting c = (ChoiceSetting) s;
				place(new KButton(x, 0, w, 20, new net.minecraft.network.chat.TextComponent(c.name + ": " + c.current()), btn -> {
					c.next();
					btn.setMessage(new net.minecraft.network.chat.TextComponent(c.name + ": " + c.current()));
					Config.save();
				}), y);
			} else if (s instanceof SliderSetting) {
				SliderSetting sl = (SliderSetting) s;
				place(new KSlider(x, 0, w, sl, Config::save), y);
			} else if (s instanceof ColorSetting) {
				ColorSetting col = (ColorSetting) s;
				labels.add(new String[] {col.name, String.valueOf(y)});
				int size = 14, per = Math.max(1, (w + 3) / (size + 3));
				for (int i = 0; i < ColorSetting.PALETTE.size(); i++) {
					int argb = ColorSetting.PALETTE.get(i);
					int sx = x + (i % per) * (size + 3);
					place(new Swatch(sx, 0, size, argb, () -> {
						col.set(argb);
						Config.save();
					}, () -> col.argb() == argb), y + 11 + (i / per) * (size + 3));
				}
				y += 11 + ((ColorSetting.PALETTE.size() + per - 1) / per) * (size + 3) - ROW + 4;
			}
			y += ROW;
		}
		contentH = y;
		layout();
	}

	private void place(AbstractWidget w, int y) {
		placed.add(new Placed(w, y));
		addButton(w);
	}

	private int top() {
		return py + 34;
	}

	private int bottom() {
		return py + ph - 8;
	}

	private void layout() {
		for (Placed p : placed) {
			int y = top() + p.y - scroll;
			p.widget.y = y;
			p.widget.visible = y >= top() && y + p.widget.getHeight() <= bottom();
		}
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollY) {
		scroll = Math.max(0, Math.min(Math.max(0, contentH - (bottom() - top())), scroll - (int) (scrollY * ROW)));
		layout();
		return true;
	}

	private void slide(Gfx g) {
		g.pose().translate((1 - UiStyle.since(opened, 240)) * 30, 0);
	}

	@Override
	protected void drawBackground(Gfx g, int mouseX, int mouseY, float delta) {
		UiStyle.dim(g, width, height, 1);
		g.pose().pushMatrix();
		slide(g);
		UiStyle.panel(g, px, py, pw, ph);
		g.fill(px + pw / 2 - 4, py + 34, px + pw / 2 - 3, py + ph - 8, UiStyle.LINE);
		int l = px + 10, r = px + pw / 2 - 12, t = py + 48, b = py + ph - 36;
		g.fill(l, t, r, b, 0xFF0C0F13);
		for (int gx = l + 4; gx < r; gx += 8) {
			for (int gy = t + 4; gy < b; gy += 8) g.fill(gx, gy, gx + 1, gy + 1, 0xFF1A1F27);
		}
		g.pose().popMatrix();
	}

	@Override
	protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
		g.pose().pushMatrix();
		slide(g);
		super.draw(g, mouseX, mouseY, delta);
		g.pose().pushMatrix();
		g.pose().translate(px + 10, py + 6);
		g.pose().scale(1.25F, 1.25F);
		g.renderItem(module.icon(), 0, 0);
		g.pose().popMatrix();
		g.drawString(font, module.name, px + 34, py + 12, UiStyle.TEXT, false);

		for (String[] l : labels) {
			int y = top() + Integer.parseInt(l[1]) - scroll;
			if (y >= top() && y + 9 <= bottom()) g.drawString(font, l[0], listX(), y, UiStyle.MUTED, false);
		}

		int cx = px + pw / 4, cy = py + (ph - 30) / 2 + 10;
		g.drawCenteredString(font, "Preview", cx, py + 36, UiStyle.MUTED);
		if (module == Features.CROSSHAIR) {
			Features.CROSSHAIR.draw(g, cx, cy);
		} else if (module.hud()) {
			float s = module.scale();
			int w = Math.round(module.width(minecraft) * s), h = Math.round(module.height(minecraft) * s);
			if (w > 0 && h > 0) Hud.drawScaled(g, minecraft, module, cx - w / 2, cy - h / 2);
		} else {
			g.pose().pushMatrix();
			g.pose().translate(cx - 24, cy - 24);
			g.pose().scale(3, 3);
			g.renderItem(module.icon(), 0, 0);
			g.pose().popMatrix();
		}
		g.pose().popMatrix();
	}

	@Override
	public void onClose() {
		Mc.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
