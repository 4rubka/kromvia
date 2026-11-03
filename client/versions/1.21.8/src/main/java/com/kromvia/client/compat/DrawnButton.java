package com.kromvia.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public abstract class DrawnButton extends Button {
	protected DrawnButton(int x, int y, int w, int h, Component label, OnPress onPress) {
		super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
	}

	@Override
	protected final void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
		drawContents(Gfx.of(g), mouseX, mouseY, delta);
	}

	@Override
	public final void onPress() {
		pressed();
		super.onPress();
	}

	protected void pressed() {}

	protected abstract void drawContents(Gfx g, int mouseX, int mouseY, float delta);

	protected void drawDefaultLabel(Gfx g) {
		renderString(g.raw(), Minecraft.getInstance().font, ARGB.color(alpha, active ? -1 : -6250336));
	}
}
