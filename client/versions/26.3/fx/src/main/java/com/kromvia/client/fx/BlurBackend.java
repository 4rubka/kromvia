package com.kromvia.client.fx;

import com.kromvia.client.fx.mixin.FxPostChainAccessor;
import com.kromvia.client.fx.mixin.FxPostPassAccessor;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

final class BlurBackend {
	private BlurBackend() {}

	static final boolean SUPPORTED = true;

	private static GpuBuffer retired;

	static PostChain load(Minecraft mc, String name) {
		return mc.getShaderManager().getPostChain(Identifier.fromNamespaceAndPath("kromvia", name), LevelTargetBundle.MAIN_TARGETS);
	}

	static void draw(Minecraft mc, String name, Matrix4f inverse, Matrix4f previous, Vec3 delta, Object target, Object pool) {
		PostChain chain = load(mc, name);
		if (chain == null) return;
		set(chain, inverse, previous, delta);
		chain.process((RenderTarget) target, (GraphicsResourceAllocator) pool);
	}

	private static void set(PostChain chain, Matrix4f inverse, Matrix4f previous, Vec3 delta) {
		for (PostPass pass : ((FxPostChainAccessor) chain).kromvia$passes()) {
			Map<String, GpuBuffer> uniforms = ((FxPostPassAccessor) pass).kromvia$uniforms();
			GpuBuffer old = uniforms.get("MotionBlurMatrices");
			if (old == null) continue;
			try (MemoryStack stack = MemoryStack.stackPush()) {
				Std140Builder b = Std140Builder.onStack(stack, 144);
				b.putMat4f(inverse).putMat4f(previous).putVec4((float) delta.x, (float) delta.y, (float) delta.z, 0);
				uniforms.put("MotionBlurMatrices", RenderSystem.getDevice().createBuffer(() -> "Kromvia motion blur", GpuBuffer.USAGE_UNIFORM, b.get()));
			}
			if (retired != null) retired.close();
			retired = old;
		}
	}
}
