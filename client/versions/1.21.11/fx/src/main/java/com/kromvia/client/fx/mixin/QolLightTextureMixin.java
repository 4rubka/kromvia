package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightTexture.class)
public class QolLightTextureMixin {
	@ModifyExpressionValue(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 2))
	private Object kromvia$gamma(Object vanilla) {
		return Qol.gamma(vanilla);
	}
}
