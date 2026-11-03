package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class QolLivingEntityRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
	private void kromvia$state(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
		((Qol.LivingState) state).kromvia$set(Qol.tintColumn(entity), Qol.cracks(entity));
	}

	@Inject(method = "getOverlayCoords", at = @At("RETURN"), cancellable = true)
	private static void kromvia$tint(LivingEntityRenderState state, float whiteOverlay, CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(Qol.overlay(cir.getReturnValueI(), ((Qol.LivingState) state).kromvia$tint()));
	}
}
