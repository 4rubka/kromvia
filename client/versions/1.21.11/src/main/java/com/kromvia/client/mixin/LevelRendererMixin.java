package com.kromvia.client.mixin;

import com.kromvia.client.module.Visuals;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@ModifyArg(method = "renderBlockOutline", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDLnet/minecraft/client/renderer/state/BlockOutlineRenderState;IF)V", ordinal = 1), index = 6)
	private int kromvia$outlineColor(int color) {
		return Visuals.OUTLINE.enabled ? Visuals.OUTLINE.argb() : color;
	}

	@ModifyArg(method = "renderBlockOutline", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDLnet/minecraft/client/renderer/state/BlockOutlineRenderState;IF)V", ordinal = 1), index = 7)
	private float kromvia$outlineWidth(float width) {
		return Visuals.OUTLINE.enabled ? Visuals.OUTLINE.width(width) : width;
	}
}
