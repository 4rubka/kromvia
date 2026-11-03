package com.kromvia.client.mixin;

import com.kromvia.client.module.Features;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
	@Inject(method = "submitFire", at = @At("HEAD"))
	private static void kromvia$lower(PoseStack pose, SubmitNodeCollector out, TextureAtlasSprite sprite, CallbackInfo ci) {
		if (Features.LOW_FIRE.enabled) pose.translate(0, -0.3F, 0);
	}

	@Inject(method = "submitFire", at = @At("TAIL"))
	private static void kromvia$restore(PoseStack pose, SubmitNodeCollector out, TextureAtlasSprite sprite, CallbackInfo ci) {
		if (Features.LOW_FIRE.enabled) pose.translate(0, 0.3F, 0);
	}
}
