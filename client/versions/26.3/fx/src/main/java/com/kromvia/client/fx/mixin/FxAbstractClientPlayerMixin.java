package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class FxAbstractClientPlayerMixin {
	@Inject(method = "getFieldOfViewModifier", at = @At("HEAD"), cancellable = true)
	private void kromvia$fov(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
		Float fov = Tweaks.fov((AbstractClientPlayer) (Object) this, firstPerson, effectScale);
		if (fov != null) cir.setReturnValue(fov);
	}
}
