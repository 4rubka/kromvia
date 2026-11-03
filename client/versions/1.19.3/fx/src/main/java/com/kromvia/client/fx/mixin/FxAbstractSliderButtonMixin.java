package com.kromvia.client.fx.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.ui.UiStyle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
public class FxAbstractSliderButtonMixin {
	@Shadow
	protected double value;

	@Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
	private void kromvia$fill(PoseStack raw, Minecraft mc, int mouseX, int mouseY, CallbackInfo ci) {
		ci.cancel();
		AbstractSliderButton self = (AbstractSliderButton) (Object) this;
		if (!self.active) return;
		Gfx g = Gfx.of(raw);
		int x = self.getX(), y = self.getY(), w = self.getWidth(), h = self.getHeight();
		int fill = x + (int) Math.round(value * w);
		g.fill(x, y, fill, y + h - 2, self.isHoveredOrFocused() ? UiStyle.SIDE : 0xFF2E2650);
		g.fill(Math.max(x, fill - 2), y, fill, y + h - 2, UiStyle.FRONT);
	}
}
