package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public class FxWeatherEffectRendererMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void kromvia$rain(ClientLevel level, float partial, Vec3 cam, WeatherRenderState state, CallbackInfo ci) {
		state.intensity = Tweaks.rain(state.intensity);
	}
}
