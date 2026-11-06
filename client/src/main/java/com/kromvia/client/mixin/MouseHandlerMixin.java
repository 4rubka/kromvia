package com.kromvia.client.mixin;

import com.kromvia.client.module.Clicks;
import com.kromvia.client.module.Hud;
import com.kromvia.client.module.Toggles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import com.kromvia.client.compat.Mc;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Inject(method = "onButton", at = @At("HEAD"))
	private void kromvia$countClick(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
		if (action == InputConstants.PRESS && Mc.screen() == null) {
			Clicks.press(info.button());
		}
	}

	@Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
	private void kromvia$hudClick(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
		if (action == InputConstants.PRESS && info.button() == 0 && Hud.click(Minecraft.getInstance())) ci.cancel();
	}

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void kromvia$zoomScroll(long window, double dx, double dy, CallbackInfo ci) {
		if (Mc.screen() == null && dy != 0 && Toggles.Zoom.scroll(dy)) ci.cancel();
	}
}
