package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Fx;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class FxGameRendererMixin {
	@Shadow
	@Final
	private CrossFrameResourcePool resourcePool;

	@Shadow
	@Final
	private GameRenderState gameRenderState;

	@Shadow
	@Final
	private RenderTarget mainRenderTarget;

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = At.Shift.AFTER))
	private void kromvia$motionBlur(CallbackInfo ci) {
		CameraRenderState camera = gameRenderState.levelRenderState.cameraRenderState;
		Fx.MOTION_BLUR.capture(camera.pos, camera.viewRotationMatrix, camera.projectionMatrix);
		Fx.MOTION_BLUR.render(Minecraft.getInstance(), mainRenderTarget, resourcePool);
		Fx.SATURATION.render(Minecraft.getInstance(), mainRenderTarget, resourcePool);
	}
}
