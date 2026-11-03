package com.kromvia.client.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.ui.Backgrounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
	@Shadow
	public int width;
	@Shadow
	public int height;

	@Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
	private void kromvia$background(GuiGraphics g, CallbackInfo ci) {
		Backgrounds.render(Gfx.of(g));
		g.fill(0, 0, width, height, 0x780B0D11);
		ci.cancel();
	}

	@Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
	private void kromvia$inWorld(GuiGraphics g, CallbackInfo ci) {
		g.fill(0, 0, width, height, 0x780B0D11);
		ci.cancel();
	}
}
