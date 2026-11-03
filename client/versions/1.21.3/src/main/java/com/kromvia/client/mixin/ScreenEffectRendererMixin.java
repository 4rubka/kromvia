package com.kromvia.client.mixin;

import com.kromvia.client.module.Features;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
	@Inject(method = "renderFire", at = @At("HEAD"))
	private static void kromvia$lower(Minecraft mc, PoseStack pose, CallbackInfo ci) {
		if (Features.LOW_FIRE.enabled) pose.translate(0, -0.3F, 0);
	}

	@Inject(method = "renderFire", at = @At("TAIL"))
	private static void kromvia$restore(Minecraft mc, PoseStack pose, CallbackInfo ci) {
		if (Features.LOW_FIRE.enabled) pose.translate(0, 0.3F, 0);
	}
}
