package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Fx;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FxFogRendererMixin {
	@Inject(method = "computeFogColor", at = @At("RETURN"))
	private void kromvia$ambience(Camera camera, float partial, ClientLevel level, int renderDistance, float darken, CallbackInfoReturnable<Vector4f> cir) {
		if (Fx.AMBIENCE.enabled) Fx.AMBIENCE.fog(cir.getReturnValue());
	}
}
