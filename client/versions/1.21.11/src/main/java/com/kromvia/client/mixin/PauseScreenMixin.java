package com.kromvia.client.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.ui.PauseExtras;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
	@Shadow
	@Final
	private boolean showPauseMenu;

	protected PauseScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void kromvia$extras(CallbackInfo ci) {
		if (showPauseMenu) PauseExtras.init(this, width, height, this::addRenderableWidget);
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void kromvia$player(GuiGraphics g, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (showPauseMenu) PauseExtras.render(Gfx.of(g), width, height);
	}
}
