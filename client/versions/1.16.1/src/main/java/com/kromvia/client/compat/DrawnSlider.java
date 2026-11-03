package com.kromvia.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class DrawnSlider extends AbstractSliderButton {
	protected DrawnSlider(int x, int y, int w, int h, Component label, double value) {
		super(x, y, w, h, label, value);
	}

	@Override
	public final void renderButton(PoseStack g, int mouseX, int mouseY, float delta) {
		drawWidget(Gfx.of(g), mouseX, mouseY, delta);
	}

	protected abstract void drawWidget(Gfx g, int mouseX, int mouseY, float delta);

	protected void drawScrollingLabel(Gfx g) {
		Gfx.centered(g.raw(), Minecraft.getInstance().font, getMessage(), x + width / 2, y + (height - 8) / 2, (active ? 0xFFFFFF : 0xA0A0A0) | Mth.ceil(alpha * 255) << 24);
	}
}
