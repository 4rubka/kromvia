package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Fx;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public class FxSkyRendererMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void kromvia$ambience(ClientLevel level, float partial, Camera camera, SkyRenderState state, CallbackInfo ci) {
		if (Fx.AMBIENCE.enabled) state.skyColor = Fx.AMBIENCE.sky(state.skyColor);
	}
}
