package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class QolPauseScreenMixin {
	@Shadow
	private @Nullable Button disconnectButton;

	@Inject(method = "createPauseMenu", at = @At("TAIL"))
	private void kromvia$confirm(CallbackInfo ci) {
		if (disconnectButton == null) return;
		var button = (QolButtonAccessor) disconnectButton;
		button.kromvia$setOnPress(Qol.confirmDisconnect((Screen) (Object) this, button.kromvia$onPress()));
	}
}
