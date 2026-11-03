package com.kromvia.client.fx.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.ui.UiStyle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractSliderButton.class)
public class FxAbstractSliderButtonMixin {
	@Shadow
	protected double value;

	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSliderButton;blitNineSliced(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIIIIIII)V", ordinal = 0))
	private void kromvia$track(PoseStack raw, int x, int y, int w, int h, int a, int b, int c, int d, int e, int f) {
		AbstractSliderButton self = (AbstractSliderButton) (Object) this;
		Gfx g = Gfx.of(raw);
		UiStyle.button(g, x, y, w, h, self.active, false);
		if (!self.active) return;
		int fill = x + (int) Math.round(value * w);
		g.fill(x, y, fill, y + h - 2, self.isHoveredOrFocused() ? UiStyle.SIDE : 0xFF2E2650);
		g.fill(Math.max(x, fill - 2), y, fill, y + h - 2, UiStyle.FRONT);
	}

	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSliderButton;blitNineSliced(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIIIIIII)V", ordinal = 1))
	private void kromvia$handle(PoseStack raw, int x, int y, int w, int h, int a, int b, int c, int d, int e, int f) {
	}
}
