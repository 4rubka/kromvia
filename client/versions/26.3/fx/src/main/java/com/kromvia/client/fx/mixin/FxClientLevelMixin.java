package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public class FxClientLevelMixin {
	@Inject(method = "getSkyFlashTime", at = @At("HEAD"), cancellable = true)
	private void kromvia$noFlash(CallbackInfoReturnable<Integer> cir) {
		if (Tweaks.hides(Tweaks.WEATHER.lightning)) cir.setReturnValue(0);
	}
}
