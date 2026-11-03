package com.kromvia.client.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public abstract class DrawnSlider extends AbstractSliderButton {
	protected DrawnSlider(int x, int y, int w, int h, Component label, double value) {
		super(x, y, w, h, label, value);
	}

	@Override
	public final void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		drawWidget(Gfx.of(g), mouseX, mouseY, delta);
	}

	protected abstract void drawWidget(Gfx g, int mouseX, int mouseY, float delta);

	protected void drawScrollingLabel(Gfx g) {
		extractScrollingStringOverContents(g.raw().textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE), getMessage(), 2);
	}
}
