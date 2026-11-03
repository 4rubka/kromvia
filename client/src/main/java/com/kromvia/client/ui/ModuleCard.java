package com.kromvia.client.ui;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.DrawnButton;
import com.kromvia.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public class ModuleCard extends DrawnButton {
	public static final int W = 104;
	public static final int H = 58;
	private final Module module;
	private float hover;
	private float knob;
	private float pulse;
	private long revealAt = System.currentTimeMillis();
	private int clipTop = Integer.MIN_VALUE, clipBottom = Integer.MAX_VALUE;

	public ModuleCard(int x, int y, Module module, OnPress onPress) {
		super(x, y, W, H, Component.literal(module.name), onPress);
		this.module = module;
		this.knob = module.enabled ? 1 : 0;
	}

	public void reveal(long at) {
		revealAt = at;
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
		pulse = 1;
	}

	@Override
	protected void drawContents(Gfx g, int mouseX, int mouseY, float delta) {
		float in = UiStyle.since(revealAt, 320);
		if (in <= 0) return;
		Minecraft mc = Minecraft.getInstance();
		boolean on = module.enabled;
		hover = Mth.lerp(0.25F, hover, isHovered() ? 1F : 0F);
		knob = Mth.lerp(0.3F, knob, on ? 1F : 0F);
		pulse = Mth.lerp(0.12F, pulse, 0);

		int x = getX(), w = getWidth(), h = getHeight();
		int y = getY() + Math.round((1 - in) * 14 - hover * 2);

		g.pose().pushMatrix();
		float s = 0.92F + 0.08F * in + pulse * 0.03F;
		g.pose().translate(x + w / 2F, y + h / 2F);
		g.pose().scale(s, s);
		g.pose().translate(-(x + w / 2F), -(y + h / 2F));

		if (knob > 0.02F) UiStyle.shadow(g, x, y, w, h, 4);
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, UiStyle.fade(ARGB.srgbLerp(knob, 0x00000000, 0x555B3FD6), in));
		int face = ARGB.srgbLerp(knob, UiStyle.CARD, 0xFF130F24);
		face = ARGB.srgbLerp(hover * 0.5F, face, 0xFF161B22);
		g.fill(x, y, x + w, y + h, UiStyle.fade(face, in));
		g.fillGradient(x, y, x + w, y + h / 2, UiStyle.fade(0x10FFFFFF, in * (0.4F + hover * 0.6F)), 0);
		int edge = ARGB.srgbLerp(knob, ARGB.srgbLerp(hover, UiStyle.LINE, 0xFF353D4A), UiStyle.FRONT);
		g.fill(x, y, x + w, y + 1, UiStyle.fade(edge, in));
		g.fill(x, y + h - 1, x + w, y + h, UiStyle.fade(0xFF050608, in));
		if (pulse > 0.02F) g.fill(x, y, x + w, y + h, ARGB.color(Math.round(pulse * 70), UiStyle.TOP));

		if (in > 0.35F) {
			float bob = hover * Mth.sin((System.currentTimeMillis() % 1200) / 1200F * Mth.TWO_PI) * 1.2F;
			float is = 1.5F + hover * 0.2F;
			g.pose().pushMatrix();
			g.pose().translate(x + w / 2F, y + 18 + bob);
			g.pose().scale(is, is);
			g.renderItem(module.icon(), -8, -8);
			g.pose().popMatrix();
		}

		String name = module.name;
		int max = w - 8;
		while (mc.font.width(name) > max && name.length() > 3) name = name.substring(0, name.length() - 2) + "…";
		int text = ARGB.srgbLerp(Math.max(knob, hover), UiStyle.MUTED, UiStyle.TEXT);
		g.drawCenteredString(mc.font, name, x + w / 2, y + 35, UiStyle.fade(text, in));

		int sx = x + w / 2 - 10, sy = y + h - 12;
		g.fill(sx, sy, sx + 20, sy + 7, UiStyle.fade(ARGB.srgbLerp(knob, 0xFF232832, UiStyle.SIDE), in));
		int kx = sx + 1 + Math.round(knob * 12);
		g.fill(kx, sy + 1, kx + 7, sy + 6, UiStyle.fade(ARGB.srgbLerp(knob, UiStyle.MUTED, UiStyle.TOP), in));
		g.pose().popMatrix();
	}
}
