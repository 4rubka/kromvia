package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class FxGuiMixin {
	@Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
	private void kromvia$portal(PoseStack g, float alpha, CallbackInfo ci) {
		if (Tweaks.OVERLAYS.hides(Tweaks.OVERLAYS.portal)) ci.cancel();
	}

	@Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
	private void kromvia$spyglass(PoseStack g, float scale, CallbackInfo ci) {
		if (Tweaks.OVERLAYS.hides(Tweaks.OVERLAYS.spyglass)) ci.cancel();
	}
}
