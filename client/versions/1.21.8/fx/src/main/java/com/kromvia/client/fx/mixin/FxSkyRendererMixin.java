package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Fx;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public class FxSkyRendererMixin {
	@Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
	private void kromvia$ambience(net.minecraft.world.phys.Vec3 pos, float partial, CallbackInfoReturnable<Integer> cir) {
		if (Fx.AMBIENCE.enabled) cir.setReturnValue(Fx.AMBIENCE.sky(cir.getReturnValueI()));
	}
}
