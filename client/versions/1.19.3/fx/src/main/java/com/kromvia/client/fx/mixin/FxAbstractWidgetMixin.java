package com.kromvia.client.fx.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractWidget.class)
public class FxAbstractWidgetMixin {
	@Redirect(method = "renderButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
	private void kromvia$fit(PoseStack pose, Font font, Component text, int x, int y, int color) {
		AbstractWidget self = (AbstractWidget) (Object) this;
		int room = self.getWidth() - 4, width = font.width(text);
		float s = room / (float) width;
		if (width <= room || s < 0.6F) {
			GuiComponent.drawCenteredString(pose, font, text, x, y, color);
			return;
		}
		float cy = self.getY() + self.getHeight() / 2F;
		pose.pushPose();
		pose.translate(x, cy, 0);
		pose.scale(s, s, 1);
		pose.translate(-x, -cy, 0);
		GuiComponent.drawCenteredString(pose, font, text, x, Math.round(cy - 4), color);
		pose.popPose();
	}
}
