package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import net.minecraft.client.Camera;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Camera.class)
public class FxCameraMixin {
	@ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true)
	private float kromvia$distance(float distance) {
		return Tweaks.cameraDistance(distance);
	}

	@ModifyExpressionValue(method = {"update", "createProjectionMatrixForCulling"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getWidth()I"))
	private int kromvia$stretched(int width) {
		return com.kromvia.client.fx.Tweaks.projectionWidth(width);
	}
}
