package com.kromvia.client.ui;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.DrawnSlider;
import com.kromvia.client.module.setting.SliderSetting;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class KSlider extends DrawnSlider {
	private final SliderSetting setting;
	private final Runnable changed;

	public KSlider(int x, int y, int w, SliderSetting setting, Runnable changed) {
		super(x, y, w, 20, Component.literal(setting.label()), setting.fraction());
		this.setting = setting;
		this.changed = changed;
	}

	@Override
	protected void updateMessage() {
		setMessage(Component.literal(setting.label()));
	}

	@Override
	protected void applyValue() {
		setting.setFraction(value);
		changed.run();
	}

	@Override
	protected void drawWidget(Gfx g, int mouseX, int mouseY, float delta) {
		int x = getX(), y = getY(), w = getWidth(), h = getHeight();
		UiStyle.button(g, x, y, w, h, true, false);
		int fill = x + (int) Math.round(value * w);
		g.fill(x, y, fill, y + h - 2, isHoveredOrFocused() ? UiStyle.SIDE : 0xFF2E2650);
		g.fill(Math.max(x, fill - 2), y, fill, y + h - 2, UiStyle.FRONT);
		drawScrollingLabel(g);
	}
}
