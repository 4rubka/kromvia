package com.kromvia.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class DrawnButton extends Button {
	protected DrawnButton(int x, int y, int w, int h, Component label, OnPress onPress) {
		super(x, y, w, h, label, onPress);
	}

	@Override
	public final void renderButton(PoseStack g, int mouseX, int mouseY, float delta) {
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
		Gfx.centered(g.raw(), Minecraft.getInstance().font, getMessage(), x + width / 2, y + (height - 8) / 2, (active ? 0xFFFFFF : 0xA0A0A0) | Mth.ceil(alpha * 255) << 24);
	}
}
