package com.kromvia.client.fx.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FishingHookRenderer.class)
public interface FishingHookRendererInvoker {
	@Invoker("stringVertex")
	static void stringVertex(float x, float y, float z, VertexConsumer buffer, PoseStack.Pose pose, float from, float to) {
		throw new AssertionError();
	}
}
