package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public abstract class QolGameRendererMixin {
	@Shadow
	protected abstract void bobView(CameraRenderState camera, PoseStack pose);

	@Shadow
	protected abstract void bobHurt(CameraRenderState camera, PoseStack pose);

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
	private void kromvia$screenBob(GameRenderer self, CameraRenderState camera, PoseStack pose) {
		if (Qol.BOBBING.on(Qol.BOBBING.screen)) bobView(camera, pose);
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
	private void kromvia$screenTilt(GameRenderer self, CameraRenderState camera, PoseStack pose) {
		if (Qol.BOBBING.on(Qol.BOBBING.screenTilt)) bobHurt(camera, pose);
	}

	@Redirect(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
	private void kromvia$handBob(GameRenderer self, CameraRenderState camera, PoseStack pose) {
		if (Qol.BOBBING.on(Qol.BOBBING.hand)) bobView(camera, pose);
	}

	@Redirect(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
	private void kromvia$handTilt(GameRenderer self, CameraRenderState camera, PoseStack pose) {
		if (Qol.BOBBING.on(Qol.BOBBING.handTilt)) bobHurt(camera, pose);
	}
}
