package com.kromvia.client.mixin;

import com.kromvia.client.Binds;
import com.kromvia.client.ui.ModuleMenu;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import com.kromvia.client.compat.Mc;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void kromvia$openMenu(long window, int action, KeyEvent event, CallbackInfo ci) {
		if (action == InputConstants.PRESS && Binds.MENU.matches(event) && Mc.screen() == null) {
			Mc.setScreen(new ModuleMenu());
			ci.cancel();
		}
	}
}
