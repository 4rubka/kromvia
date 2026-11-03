package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Fx;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FxFogRendererMixin {
	@Shadow
	private static float fogRed;

	@Shadow
	private static float fogGreen;

	@Shadow
	private static float fogBlue;

	@Inject(method = "setupColor", at = @At("TAIL"))
	private static void kromvia$ambience(Camera camera, float partial, ClientLevel level, int renderDistance, float darken, CallbackInfo ci) {
		if (!Fx.AMBIENCE.enabled) return;
		Vector4f rgba = new Vector4f(fogRed, fogGreen, fogBlue, 1);
		Fx.AMBIENCE.fog(rgba);
		fogRed = rgba.x;
		fogGreen = rgba.y;
		fogBlue = rgba.z;
		RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
	}
}
