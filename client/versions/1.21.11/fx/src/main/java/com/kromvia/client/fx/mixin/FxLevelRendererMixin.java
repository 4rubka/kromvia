package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Fx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class FxLevelRendererMixin {
	@Inject(method = "submitEntities", at = @At("TAIL"))
	private void kromvia$worldFx(PoseStack pose, LevelRenderState state, SubmitNodeCollector out, CallbackInfo ci) {
		Fx.submitWorld(pose, out, state.cameraRenderState.pos);
	}

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void kromvia$motionBlurCamera(GraphicsResourceAllocator allocator, DeltaTracker delta, boolean outline, Camera camera, Matrix4f view, Matrix4f projection,
			Matrix4f culling, GpuBufferSlice fog, Vector4f fogColor, boolean thick, CallbackInfo ci) {
		Fx.MOTION_BLUR.capture(camera.position(), view, projection);
	}
}
