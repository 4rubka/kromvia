package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class FxHudMixin {
	@Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
	private void kromvia$portal(GuiGraphicsExtractor g, float alpha, CallbackInfo ci) {
		if (Tweaks.OVERLAYS.hides(Tweaks.OVERLAYS.portal)) ci.cancel();
	}

	@Inject(method = "extractSpyglassOverlay", at = @At("HEAD"), cancellable = true)
	private void kromvia$spyglass(GuiGraphicsExtractor g, float scale, CallbackInfo ci) {
		if (Tweaks.OVERLAYS.hides(Tweaks.OVERLAYS.spyglass)) ci.cancel();
	}
}
