package com.kromvia.client.fx.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public class FxAbstractWidgetMixin {
	@Inject(method = "renderScrollingString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;II)V", at = @At("HEAD"), cancellable = true)
	private void kromvia$fit(PoseStack pose, Font font, int margin, int color, CallbackInfo ci) {
		AbstractWidget self = (AbstractWidget) (Object) this;
		int room = self.getWidth() - margin * 2, width = font.width(self.getMessage());
		if (width <= room) return;
		float s = room / (float) width;
		if (s < 0.6F) return;
		float cx = self.getX() + self.getWidth() / 2F, cy = self.getY() + self.getHeight() / 2F;
		pose.pushPose();
		pose.translate(cx, cy, 0);
		pose.scale(s, s, 1);
		pose.translate(-cx, -cy, 0);
		GuiComponent.drawCenteredString(pose, font, self.getMessage(), Math.round(cx), Math.round(cy - 4), color);
		pose.popPose();
		ci.cancel();
	}
}
