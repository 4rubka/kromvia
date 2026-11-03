package com.kromvia.client.mixin;

import com.kromvia.client.module.Visuals;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
	@Unique
	private static final float FAR = 1_000_000F;
	@Unique
	private boolean kromvia$clear;

	@Inject(method = "setupFog", at = @At("HEAD"))
	private void kromvia$check(Camera camera, int distance, boolean thick, DeltaTracker delta, float darken, ClientLevel level, CallbackInfoReturnable<Vector4f> cir) {
		kromvia$clear = Visuals.NO_FOG.enabled && camera.getFluidInCamera() == FogType.NONE
				&& !(camera.getEntity() instanceof LivingEntity e && (e.hasEffect(MobEffects.BLINDNESS) || e.hasEffect(MobEffects.DARKNESS)));
	}

	@ModifyArg(method = "setupFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"), index = 3)
	private float kromvia$envStart(float v) {
		return kromvia$clear ? FAR : v;
	}

	@ModifyArg(method = "setupFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"), index = 4)
	private float kromvia$envEnd(float v) {
		return kromvia$clear ? FAR : v;
	}

	@ModifyArg(method = "setupFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"), index = 5)
	private float kromvia$distStart(float v) {
		return kromvia$clear ? FAR : v;
	}

	@ModifyArg(method = "setupFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"), index = 6)
	private float kromvia$distEnd(float v) {
		return kromvia$clear ? FAR : v;
	}
}
