package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingHookRenderer.class)
public class FxFishingHookRendererMixin {
	@WrapOperation(require = 0, method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/FishingHookRenderer;stringVertex(FFFLcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/math/Matrix4f;F)V"))
	private void kromvia$line(float x, float y, float z, VertexConsumer buffer, Matrix4f matrix, float fraction, Operation<Void> op) {
		if (!Tweaks.hides(Tweaks.WEATHER.fishingLine)) op.call(x, y, z, buffer, matrix, fraction);
	}
}
