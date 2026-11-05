package com.kromvia.client.mixin;

import com.kromvia.client.module.EasyPlace;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class EasyPlaceMixin {
	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void kromvia$easyPlace(CallbackInfo ci) {
		if (EasyPlace.intercept()) ci.cancel();
	}
}
