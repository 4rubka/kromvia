package com.kromvia.client.mixin;

import com.kromvia.client.module.Features;
import com.kromvia.client.module.Toggles;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {
	@Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
	private float kromvia$yaw(Entity entity, float delta) {
		return Features.Freelook.looking() ? Features.Freelook.yaw : entity.getViewYRot(delta);
	}

	@Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
	private float kromvia$pitch(Entity entity, float delta) {
		return Features.Freelook.looking() ? Features.Freelook.pitch : entity.getViewXRot(delta);
	}

	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void kromvia$zoom(float delta, CallbackInfoReturnable<Float> cir) {
		float f = Toggles.Zoom.factor();
		if (f != 1) cir.setReturnValue(cir.getReturnValue() * f);
	}
}
