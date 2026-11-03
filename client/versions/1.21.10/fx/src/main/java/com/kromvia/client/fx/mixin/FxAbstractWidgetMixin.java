package com.kromvia.client.fx.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public class FxAbstractWidgetMixin {
	@Inject(method = "renderScrollingString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;II)V", at = @At("HEAD"), cancellable = true)
	private void kromvia$fit(GuiGraphics g, Font font, int margin, int color, CallbackInfo ci) {
		AbstractWidget self = (AbstractWidget) (Object) this;
		int room = self.getWidth() - margin * 2, width = font.width(self.getMessage());
		if (width <= room) return;
		float s = room / (float) width;
		if (s < 0.6F) return;
		float cx = self.getX() + self.getWidth() / 2F, cy = self.getY() + self.getHeight() / 2F;
		g.pose().pushMatrix();
		g.pose().translate(cx, cy).scale(s).translate(-cx, -cy);
		g.drawCenteredString(font, self.getMessage(), Math.round(cx), Math.round(cy - 4), color);
		g.pose().popMatrix();
		ci.cancel();
	}
}
