package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class QolMouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private boolean mouseGrabbed;

	@Shadow
	private double xpos;

	@Shadow
	private double ypos;

	@Unique
	private boolean kromvia$wasGrabbed;

	@Inject(method = "grabMouse", at = @At("HEAD"))
	private void kromvia$remember(CallbackInfo ci) {
		if (!mouseGrabbed) Qol.cursorTaken(xpos, ypos);
	}

	@Inject(method = "releaseMouse", at = @At("HEAD"))
	private void kromvia$wasGrabbed(CallbackInfo ci) {
		kromvia$wasGrabbed = mouseGrabbed;
	}

	@Inject(method = "releaseMouse", at = @At("TAIL"))
	private void kromvia$restore(CallbackInfo ci) {
		double[] back = kromvia$wasGrabbed ? Qol.cursorBack() : null;
		if (back == null) return;
		xpos = back[0];
		ypos = back[1];
		InputConstants.releaseMouse(minecraft.getWindow(), xpos, ypos);
	}
}
