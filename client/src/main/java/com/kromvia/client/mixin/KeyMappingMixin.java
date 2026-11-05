package com.kromvia.client.mixin;

import com.kromvia.client.module.Freecam;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {
	@Shadow private int clickCount;

	@Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
	private void kromvia$freecamDown(CallbackInfoReturnable<Boolean> cir) {
		if (Freecam.blocks((KeyMapping) (Object) this)) cir.setReturnValue(false);
	}

	@Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
	private void kromvia$freecamClick(CallbackInfoReturnable<Boolean> cir) {
		if (Freecam.blocks((KeyMapping) (Object) this)) {
			clickCount = 0;
			cir.setReturnValue(false);
		}
	}
}
