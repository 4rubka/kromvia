package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CameraType.class)
public class FxCameraTypeMixin {
	@Inject(method = "cycle", at = @At("RETURN"), cancellable = true)
	private void kromvia$skipFront(CallbackInfoReturnable<CameraType> cir) {
		cir.setReturnValue(Tweaks.cycled(cir.getReturnValue()));
	}
}
