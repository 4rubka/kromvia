package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class QolPauseScreenMixin extends Screen {
	protected QolPauseScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "createPauseMenu", at = @At("TAIL"))
	private void kromvia$confirm(CallbackInfo ci) {
		for (AbstractWidget w : buttons) {
			if (w instanceof Button && w.x == width / 2 - 102 && w.y == height / 4 + 120 - 16) {
				QolButtonAccessor button = (QolButtonAccessor) w;
				button.kromvia$setOnPress(Qol.confirmDisconnect(this, button.kromvia$onPress()));
			}
		}
	}
}
