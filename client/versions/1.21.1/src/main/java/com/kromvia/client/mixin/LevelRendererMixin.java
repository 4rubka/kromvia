package com.kromvia.client.mixin;

import com.kromvia.client.compat.ARGB;
import com.kromvia.client.module.Visuals;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@Unique
	private static final String SHAPE = "Lnet/minecraft/client/renderer/LevelRenderer;renderShape(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/phys/shapes/VoxelShape;DDDFFFF)V";

	@ModifyArg(method = "renderHitOutline", at = @At(value = "INVOKE", target = SHAPE), index = 6)
	private float kromvia$red(float red) {
		return Visuals.OUTLINE.enabled ? (Visuals.OUTLINE.argb() >> 16 & 0xFF) / 255F : red;
	}

	@ModifyArg(method = "renderHitOutline", at = @At(value = "INVOKE", target = SHAPE), index = 7)
	private float kromvia$green(float green) {
		return Visuals.OUTLINE.enabled ? (Visuals.OUTLINE.argb() >> 8 & 0xFF) / 255F : green;
	}

	@ModifyArg(method = "renderHitOutline", at = @At(value = "INVOKE", target = SHAPE), index = 8)
	private float kromvia$blue(float blue) {
		return Visuals.OUTLINE.enabled ? (Visuals.OUTLINE.argb() & 0xFF) / 255F : blue;
	}

	@ModifyArg(method = "renderHitOutline", at = @At(value = "INVOKE", target = SHAPE), index = 9)
	private float kromvia$alpha(float alpha) {
		return Visuals.OUTLINE.enabled ? ARGB.alpha(Visuals.OUTLINE.argb()) / 255F : alpha;
	}
}
