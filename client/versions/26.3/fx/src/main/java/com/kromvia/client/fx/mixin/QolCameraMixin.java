package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public class QolCameraMixin {
	@ModifyExpressionValue(method = "calculateFov", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 0))
	private Object kromvia$fov(Object vanilla) {
		return Qol.fov(vanilla);
	}
}
