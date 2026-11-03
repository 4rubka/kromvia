package com.kromvia.client.mixin;

import com.kromvia.client.module.Visuals;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
	@Unique
	private static final float FAR = 1_000_000F;

	@Inject(method = "setupFog", at = @At("RETURN"))
	private void kromvia$clear(Camera camera, int distance, DeltaTracker delta, float darken, ClientLevel level, CallbackInfoReturnable<FogData> cir) {
		boolean clear = Visuals.NO_FOG.enabled && camera.getFluidInCamera() == FogType.NONE
				&& !(camera.entity() instanceof LivingEntity e && (e.hasEffect(MobEffects.BLINDNESS) || e.hasEffect(MobEffects.DARKNESS)));
		if (!clear) return;
		FogData fog = cir.getReturnValue();
		fog.environmentalStart = FAR;
		fog.environmentalEnd = FAR;
		fog.renderDistanceStart = FAR;
		fog.renderDistanceEnd = FAR;
	}
}
