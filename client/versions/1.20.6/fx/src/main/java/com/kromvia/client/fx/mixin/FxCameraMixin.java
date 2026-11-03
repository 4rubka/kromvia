package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Camera.class)
public class FxCameraMixin {
	@ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true)
	private double kromvia$distance(double distance) {
		return Tweaks.cameraDistance((float) distance);
	}
}
