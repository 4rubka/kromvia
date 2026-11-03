package com.kromvia.client.mixin;

import com.kromvia.client.module.Features;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.ClientLevelData.class)
public class ClientLevelDataMixin {
	@Inject(method = "getDayTime", at = @At("RETURN"), cancellable = true)
	private void kromvia$time(CallbackInfoReturnable<Long> cir) {
		if (Features.TIME.enabled) cir.setReturnValue(Features.fixedTime());
	}
}
