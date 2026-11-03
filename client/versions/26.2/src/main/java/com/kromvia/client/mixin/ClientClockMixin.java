package com.kromvia.client.mixin;

import com.kromvia.client.module.Features;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
public class ClientClockMixin {
	@Inject(method = "getTotalTicks", at = @At("RETURN"), cancellable = true)
	private void kromvia$time(CallbackInfoReturnable<Long> cir) {
		if (Features.TIME.enabled) cir.setReturnValue(Features.fixedTime());
	}
}
