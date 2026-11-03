package com.kromvia.client.ui;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.DrawnButton;
import java.util.function.BooleanSupplier;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class KButton extends DrawnButton {
	private static final float SLIDE_MS = 260;
	private final BooleanSupplier selected;
	private final long born = System.currentTimeMillis();
	private int delay;
	private float hover;
	private float press;
	private boolean flat;
	private int clipTop = Integer.MIN_VALUE, clipBottom = Integer.MAX_VALUE;

	public KButton(int x, int y, int w, int h, Component label, OnPress onPress, BooleanSupplier selected) {
		super(x, y, w, h, label, onPress);
		this.selected = selected;
	}

	public KButton(int x, int y, int w, int h, Component label, OnPress onPress) {
		this(x, y, w, h, label, onPress, () -> false);
	}

	public KButton appear(int delayMs) {
		delay = delayMs;
		return this;
	}

	public KButton flat() {
		flat = true;
		return this;
	}

	public void clip(int top, int bottom) {
		clipTop = top;
		clipBottom = bottom;
	}

	@Override
	public boolean isMouseOver(double x, double y) {
		return y >= clipTop && y < clipBottom && super.isMouseOver(x, y);
	}

	@Override
	protected void pressed() {
		press = 1;
	}

	protected float entrance() {
		float t = Mth.clamp((System.currentTimeMillis() - born - delay) / SLIDE_MS, 0, 1);
		return 1 - (1 - t) * (1 - t) * (1 - t);
	}

	@Override
	protected void drawContents(Gfx g, int mouseX, int mouseY, float delta) {
		float in = entrance();
		if (in <= 0) return;
		hover = Mth.lerp(0.35F, hover, isHoveredOrFocused() ? 1F : 0F);
		press = Mth.lerp(0.15F, press, 0);
		g.pose().pushMatrix();
		g.pose().translate((1 - in) * -24, 0);
		if (flat) {
			if (hover > 0.02F) g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), UiStyle.fade(0x22FFFFFF, hover));
		} else {
			boolean lit = selected.getAsBoolean() || isHoveredOrFocused();
			UiStyle.button(g, getX(), getY(), getWidth(), getHeight(), active, lit);
			int bar = Math.round(hover * (getHeight() - 2));
			if (bar > 0) g.fill(getX(), getY() + (getHeight() - 2 - bar) / 2, getX() + 2, getY() + (getHeight() - 2 + bar) / 2, UiStyle.TOP);
		}
		if (press > 0.02F) g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), UiStyle.fade(0x66B49CFF, press));
		g.pose().translate(hover * 2, 0);
		drawDefaultLabel(g);
		g.pose().popMatrix();
	}
}
