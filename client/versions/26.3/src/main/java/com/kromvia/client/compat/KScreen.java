package com.kromvia.client.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public abstract class KScreen extends Screen {
	protected KScreen(Component title) {
		super(title);
	}

	@Override
	public final void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		draw(Gfx.of(g), mouseX, mouseY, delta);
	}

	@Override
	public final void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		drawBackground(Gfx.of(g), mouseX, mouseY, delta);
	}

	protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g.raw(), mouseX, mouseY, delta);
	}

	protected void drawBackground(Gfx g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g.raw(), mouseX, mouseY, delta);
	}

	protected void drawPanorama(Gfx g, float delta) {
		extractPanorama(g.raw(), delta);
	}

	@Override
	public final boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		mouseDown(event.x(), event.y());
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public final boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		return mouseDrag(dx, dy) || super.mouseDragged(event, dx, dy);
	}

	@Override
	public final boolean mouseReleased(MouseButtonEvent event) {
		mouseUp();
		return super.mouseReleased(event);
	}

	@Override
	public final boolean keyPressed(KeyEvent event) {
		return keyDown(event.key()) || super.keyPressed(event);
	}

	protected void mouseDown(double x, double y) {}

	protected boolean mouseDrag(double dx, double dy) {
		return false;
	}

	protected void mouseUp() {}

	protected boolean keyDown(int key) {
		return false;
	}
}
