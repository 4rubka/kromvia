package com.kromvia.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public abstract class DrawnSlider extends AbstractSliderButton {
	protected DrawnSlider(int x, int y, int w, int h, Component label, double value) {
		super(x, y, w, h, label, value);
	}

	@Override
	public final void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
		drawWidget(Gfx.of(g), mouseX, mouseY, delta);
	}

	protected abstract void drawWidget(Gfx g, int mouseX, int mouseY, float delta);

	protected void drawScrollingLabel(Gfx g) {
		renderScrollingString(g.raw(), Minecraft.getInstance().font, 2, ARGB.color(alpha, active ? -1 : -6250336));
	}
}
