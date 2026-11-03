package com.kromvia.client.fx.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public class FxAbstractWidgetMixin {
	@Inject(method = "extractScrollingStringOverContents", at = @At("HEAD"), cancellable = true)
	private void kromvia$fit(ActiveTextCollector out, Component message, int margin, CallbackInfo ci) {
		AbstractWidget self = (AbstractWidget) (Object) this;
		int room = self.getWidth() - margin * 2, width = Minecraft.getInstance().font.width(message);
		if (width <= room) return;
		float s = room / (float) width;
		if (s < 0.6F) return;
		float cx = self.getX() + self.getWidth() / 2F, cy = self.getY() + self.getHeight() / 2F;
		var p = out.defaultParameters();
		Matrix3x2f pose = new Matrix3x2f(p.pose()).translate(cx, cy).scale(s).translate(-cx, -cy);
		int y = Math.round(cy - 4);
		out.accept(TextAlignment.CENTER, Math.round(cx), y, p.withPose(pose), message.getVisualOrderText());
		ci.cancel();
	}
}
