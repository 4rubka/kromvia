package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class QolLivingEntityRendererMixin {
	@Inject(method = "getOverlayCoords", at = @At("RETURN"), cancellable = true)
	private static void kromvia$tint(LivingEntity entity, float whiteOverlay, CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(Qol.overlay(cir.getReturnValueI(), Qol.tintColumn(entity)));
	}
}
