package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class FxScreenOverlayMixin {
	@ModifyArg(method = "buildFireQuad", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;buildSpriteQuad(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;FFFFFI)V"), index = 8)
	private static int kromvia$fire(int argb) {
		return Tweaks.OVERLAYS.fire(argb);
	}

	@ModifyConstant(method = "lambda$submitFire$0", constant = @Constant(floatValue = -0.3F))
	private static float kromvia$fireHeight(float y) {
		return Tweaks.OVERLAYS.fireY(y);
	}

	@Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
	private static void kromvia$water(PlayerRenderState.WaterOverlay water, PoseStack pose, SubmitNodeCollector out, CallbackInfo ci) {
		if (Tweaks.OVERLAYS.hides(Tweaks.OVERLAYS.water)) ci.cancel();
	}
}
