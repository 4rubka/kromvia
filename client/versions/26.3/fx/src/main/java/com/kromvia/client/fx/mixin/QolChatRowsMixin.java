package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = {"net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess", "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess"})
public abstract class QolChatRowsMixin {
	@Redirect(method = "fill(IIIII)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
	private void kromvia$background(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int argb) {
		if (!Tweaks.chatRow(g::fill, x1, y1, x2, y2, argb)) g.fill(x1, y1, x2, y2, Tweaks.chatBackground(argb));
	}
}
