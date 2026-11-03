package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class QolScreenMixin {
	@Inject(method = "renderTransparentBackground", at = @At("HEAD"))
	private void kromvia$blur(GuiGraphics g, CallbackInfo ci) {
		if ((Object) this instanceof AbstractContainerScreen<?> && Qol.blurInventory()) { Minecraft mc = Minecraft.getInstance(); mc.gameRenderer.processBlurEffect(mc.getFrameTime()); }
	}
}
