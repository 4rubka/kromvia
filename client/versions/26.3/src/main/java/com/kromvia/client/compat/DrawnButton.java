package com.kromvia.client.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public abstract class DrawnButton extends Button {
	protected DrawnButton(int x, int y, int w, int h, Component label, OnPress onPress) {
		super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
	}

	@Override
	protected final void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		drawContents(Gfx.of(g), mouseX, mouseY, delta);
	}

	@Override
	public final void onPress(InputWithModifiers input) {
		pressed();
		super.onPress(input);
	}

	protected void pressed() {}

	protected abstract void drawContents(Gfx g, int mouseX, int mouseY, float delta);

	protected void drawDefaultLabel(Gfx g) {
		extractDefaultLabel(g.raw().textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
	}
}
