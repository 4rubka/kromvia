package com.kromvia.client.fx;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.shaders.Uniform;
import com.kromvia.client.fx.mixin.FxPostChainAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
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
		for (PostPass pass : ((FxPostChainAccessor) chain).kromvia$passes()) {
			CompiledShaderProgram shader = pass.getShader();
			Uniform inv = shader.getUniform("InvViewProj");
			if (inv == null) continue;
			inv.set(inverse);
			shader.safeGetUniform("PrevViewProj").set(previous);
			shader.safeGetUniform("Delta").set((float) delta.x, (float) delta.y, (float) delta.z);
		}
		chain.process((RenderTarget) target, (GraphicsResourceAllocator) pool);
	}
}
