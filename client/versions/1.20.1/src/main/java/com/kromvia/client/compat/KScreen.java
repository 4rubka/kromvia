package com.kromvia.client.compat;

import com.kromvia.client.ui.Backgrounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;

public abstract class KScreen extends Screen {
	protected KScreen(Component title) {
		super(title);
	}

	@Override
	public final void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		draw(Gfx.of(g), mouseX, mouseY, delta);
	}

	protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
		drawBackground(g, mouseX, mouseY, delta);
		super.render(g.raw(), mouseX, mouseY, delta);
	}

	protected void drawBackground(Gfx g, int mouseX, int mouseY, float delta) {
		super.renderBackground(g.raw());
	}

	protected void drawPanorama(Gfx g, float delta) {
		if (Backgrounds.render(g)) return;
		if (panorama == null) panorama = new PanoramaRenderer(TitleScreen.CUBE_MAP);
		panorama.render(delta, 1);
	}

	private static PanoramaRenderer panorama;

	@Override
	public final boolean mouseClicked(double x, double y, int button) {
		mouseDown(x, y);
		return super.mouseClicked(x, y, button);
	}

	@Override
	public final boolean mouseDragged(double x, double y, int button, double dx, double dy) {
		return mouseDrag(dx, dy) || super.mouseDragged(x, y, button, dx, dy);
	}

	@Override
	public final boolean mouseReleased(double x, double y, int button) {
		mouseUp();
		return super.mouseReleased(x, y, button);
	}

	@Override
	public final boolean keyPressed(int key, int scancode, int modifiers) {
		return keyDown(key) || super.keyPressed(key, scancode, modifiers);
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
