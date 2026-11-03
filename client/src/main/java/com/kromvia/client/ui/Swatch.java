package com.kromvia.client.ui;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.cosmetic.Wardrobe;
import java.util.function.BooleanSupplier;

import net.minecraft.network.chat.Component;

public class Swatch extends KButton {
	private final int color;
	private final BooleanSupplier picked;

	public Swatch(int x, int y, int size, int color, Runnable pick, BooleanSupplier picked) {
		super(x, y, size, size, Component.empty(), b -> pick.run());
		this.color = color;
		this.picked = picked;
	}

	@Override
	protected void drawContents(Gfx g, int mouseX, int mouseY, float delta) {
		if (entrance() <= 0) return;
		int grow = isHoveredOrFocused() ? 1 : 0;
		int x = getX() - grow, y = getY() - grow, s = getWidth() + grow * 2;
		int ring = picked.getAsBoolean() ? UiStyle.FRONT : isHoveredOrFocused() ? UiStyle.MUTED : UiStyle.LINE;
		g.fill(x, y, x + s, y + s, ring);
		g.fill(x + 2, y + 2, x + s - 2, y + s - 2, color == Wardrobe.RAINBOW ? Wardrobe.rainbow() : color);
	}
}
