package com.kromvia.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class DrawnSlider extends AbstractSliderButton {
	protected DrawnSlider(int x, int y, int w, int h, Component label, double value) {
		super(x, y, w, h, value);
		setMessage(label.getString());
	}

	public int getHeight() {
		return height;
	}

	@Override
	public final void renderButton(int mouseX, int mouseY, float delta) {
		drawWidget(Gfx.of(), mouseX, mouseY, delta);
	}

	protected abstract void drawWidget(Gfx g, int mouseX, int mouseY, float delta);

	protected void drawScrollingLabel(Gfx g) {
		g.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + width / 2, y + (height - 8) / 2, (active ? 0xFFFFFF : 0xA0A0A0) | Mth.ceil(alpha * 255) << 24);
	}
}
