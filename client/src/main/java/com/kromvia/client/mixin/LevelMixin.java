package com.kromvia.client.mixin;

import com.kromvia.client.module.Features;
import com.kromvia.client.module.IrlWeather;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelMixin {
	@Inject(method = "getRainLevel", at = @At("RETURN"), cancellable = true)
	private void kromvia$rain(float delta, CallbackInfoReturnable<Float> cir) {
		if (!((Level) (Object) this).isClientSide()) return;
		float irl = IrlWeather.INSTANCE.rain();
		if (Features.CLEAR_WEATHER.enabled) cir.setReturnValue(0F);
		else if (irl >= 0) cir.setReturnValue(irl);
	}

	@Inject(method = "getThunderLevel", at = @At("RETURN"), cancellable = true)
	private void kromvia$thunder(float delta, CallbackInfoReturnable<Float> cir) {
		if (!((Level) (Object) this).isClientSide()) return;
		float irl = IrlWeather.INSTANCE.thunder();
		if (Features.CLEAR_WEATHER.enabled) cir.setReturnValue(0F);
		else if (irl >= 0) cir.setReturnValue(irl);
	}
}
