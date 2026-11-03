package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Fx;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public class FxSkyRendererMixin {
	@Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
	private void kromvia$ambience(Vec3 pos, float partial, CallbackInfoReturnable<Vec3> cir) {
		if (!Fx.AMBIENCE.enabled) return;
		Vec3 sky = cir.getReturnValue();
		Vector3f rgb = new Vector3f((float) sky.x, (float) sky.y, (float) sky.z);
		Fx.AMBIENCE.sky(rgb);
		cir.setReturnValue(new Vec3(rgb.x, rgb.y, rgb.z));
	}
}
