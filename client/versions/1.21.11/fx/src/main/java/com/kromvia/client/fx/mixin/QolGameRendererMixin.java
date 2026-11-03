package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public abstract class QolGameRendererMixin {
	@Shadow
	protected abstract void bobView(PoseStack pose, float partialTicks);

	@Shadow
	protected abstract void bobHurt(PoseStack pose, float partialTicks);

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
	private void kromvia$screenBob(GameRenderer self, PoseStack pose, float partialTicks) {
		if (Qol.BOBBING.on(Qol.BOBBING.screen)) bobView(pose, partialTicks);
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
	private void kromvia$screenTilt(GameRenderer self, PoseStack pose, float partialTicks) {
		if (Qol.BOBBING.on(Qol.BOBBING.screenTilt)) bobHurt(pose, partialTicks);
	}

	@Redirect(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
	private void kromvia$handBob(GameRenderer self, PoseStack pose, float partialTicks) {
		if (Qol.BOBBING.on(Qol.BOBBING.hand)) bobView(pose, partialTicks);
	}

	@Redirect(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
	private void kromvia$handTilt(GameRenderer self, PoseStack pose, float partialTicks) {
		if (Qol.BOBBING.on(Qol.BOBBING.handTilt)) bobHurt(pose, partialTicks);
	}

	@ModifyExpressionValue(method = "getFov", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 0))
	private Object kromvia$fov(Object vanilla) {
		return Qol.fov(vanilla);
	}

	@ModifyExpressionValue(method = "getProjectionMatrix", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getWidth()I"))
	private int kromvia$stretched(int width) {
		return com.kromvia.client.fx.Tweaks.projectionWidth(width);
	}
}
