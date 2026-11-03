package com.kromvia.client.mixin;

import com.kromvia.client.module.Visuals;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
	@Unique
	private static final float FAR = 1_000_000F;

	@Inject(method = "setupFog", at = @At("TAIL"))
	private static void kromvia$clear(Camera camera, FogRenderer.FogMode mode, float distance, boolean thick, CallbackInfo ci) {
		if (Visuals.NO_FOG.enabled && camera.getFluidInCamera().isEmpty()
				&& !(camera.getEntity() instanceof LivingEntity && ((LivingEntity) camera.getEntity()).hasEffect(MobEffects.BLINDNESS))) {
			RenderSystem.fogStart(FAR);
			RenderSystem.fogEnd(FAR);
		}
	}
}
