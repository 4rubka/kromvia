package com.kromvia.client.fx;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class BlurBackend {
	private BlurBackend() {}

	static final boolean SUPPORTED = true;

	static PostChain load(Minecraft mc, String name) {
		return mc.getShaderManager().getPostChain(ResourceLocation.fromNamespaceAndPath("kromvia", name), LevelTargetBundle.MAIN_TARGETS);
	}

	static void draw(Minecraft mc, String name, Matrix4f inverse, Matrix4f previous, Vec3 delta, Object target, Object pool) {
		PostChain chain = load(mc, name);
		if (chain == null) return;
		chain.process((RenderTarget) target, (GraphicsResourceAllocator) pool, pass -> {
			pass.setUniform("InvViewProj", inverse);
			pass.setUniform("PrevViewProj", previous);
			pass.setUniform("Delta", (float) delta.x, (float) delta.y, (float) delta.z);
		});
	}
}
