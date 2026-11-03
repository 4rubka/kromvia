package com.kromvia.client.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.ui.UiStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractButton.class)
public class AbstractButtonMixin {
	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/GuiGraphics;blitNineSliced(Lnet/minecraft/resources/ResourceLocation;IIIIIIIIII)V"))
	private void kromvia$style(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h, int border, int cornerH, int texW, int texH, int u, int v) {
		AbstractButton self = (AbstractButton) (Object) this;
		UiStyle.button(Gfx.of(g), x, y, w, h, self.active, self.isHoveredOrFocused());
	}
}
