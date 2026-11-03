package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightTexture.class)
public class QolLightTextureMixin {
	@Redirect(method = "updateLightTexture", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;gamma:D"))
	private double kromvia$gamma(Options options) {
		return ((Number) Qol.gamma(options.gamma)).doubleValue();
	}
}
