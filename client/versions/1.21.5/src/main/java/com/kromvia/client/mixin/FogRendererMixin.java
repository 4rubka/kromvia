package com.kromvia.client.mixin;

import com.kromvia.client.module.Visuals;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
	@Unique
	private static final float FAR = 1_000_000F;

	@Inject(method = "setupFog", at = @At("RETURN"), cancellable = true)
	private static void kromvia$clear(Camera camera, FogRenderer.FogMode mode, Vector4f color, float distance, boolean thick, float delta, CallbackInfoReturnable<FogParameters> cir) {
		if (Visuals.NO_FOG.enabled && camera.getFluidInCamera() == FogType.NONE
				&& !(camera.getEntity() instanceof LivingEntity e && (e.hasEffect(MobEffects.BLINDNESS) || e.hasEffect(MobEffects.DARKNESS)))) {
			FogParameters f = cir.getReturnValue();
			cir.setReturnValue(new FogParameters(FAR, FAR, f.shape(), f.red(), f.green(), f.blue(), f.alpha()));
		}
	}
}
