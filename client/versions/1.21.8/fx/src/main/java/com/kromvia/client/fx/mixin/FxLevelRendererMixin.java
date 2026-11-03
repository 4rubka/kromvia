package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Fx;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class FxLevelRendererMixin {
	@Inject(method = "renderEntities", at = @At("TAIL"))
	private void kromvia$worldFx(PoseStack pose, MultiBufferSource.BufferSource out, Camera camera, DeltaTracker delta, List<Entity> entities, CallbackInfo ci) {
		Fx.submitWorld(pose, out, camera.getPosition());
	}

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void kromvia$motionBlurCamera(GraphicsResourceAllocator allocator, DeltaTracker delta, boolean outline, Camera camera, Matrix4f view, Matrix4f projection,
			GpuBufferSlice fog, Vector4f fogColor, boolean thick, CallbackInfo ci) {
		Fx.MOTION_BLUR.capture(camera.getPosition(), view, projection);
	}
}
