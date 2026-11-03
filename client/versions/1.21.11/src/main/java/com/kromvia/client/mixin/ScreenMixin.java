package com.kromvia.client.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.ui.Backgrounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
	@Inject(method = "renderMenuBackground(Lnet/minecraft/client/gui/GuiGraphics;IIII)V", at = @At("HEAD"), cancellable = true)
	private void kromvia$background(GuiGraphics g, int x, int y, int w, int h, CallbackInfo ci) {
		g.fill(x, y, x + w, y + h, 0x780B0D11);
		ci.cancel();
	}

	@Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
	private void kromvia$noBlur(GuiGraphics g, CallbackInfo ci) {
		if (net.minecraft.client.Minecraft.getInstance().level == null) ci.cancel();
	}

	@Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
	private void kromvia$panorama(GuiGraphics g, float delta, CallbackInfo ci) {
		if (Backgrounds.render(Gfx.of(g))) ci.cancel();
	}
}
