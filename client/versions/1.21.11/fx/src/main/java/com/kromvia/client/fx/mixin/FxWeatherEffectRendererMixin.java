package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public class FxWeatherEffectRendererMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void kromvia$rain(Level level, int ticks, float partial, Vec3 cam, WeatherRenderState state, CallbackInfo ci) {
		state.intensity = Tweaks.rain(state.intensity);
	}
}
