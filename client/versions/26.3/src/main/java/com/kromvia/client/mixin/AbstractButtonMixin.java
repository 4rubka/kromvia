package com.kromvia.client.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.ui.UiStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public class AbstractButtonMixin {
	@Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
	private void kromvia$style(GuiGraphicsExtractor g, CallbackInfo ci) {
		AbstractButton self = (AbstractButton) (Object) this;
		UiStyle.button(Gfx.of(g), self.getX(), self.getY(), self.getWidth(), self.getHeight(), self.active, self.isHoveredOrFocused());
		ci.cancel();
	}
}
