package com.kromvia.client.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.ui.Backgrounds;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
	@Shadow
	public int width;
	@Shadow
	public int height;

	@Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
	private void kromvia$background(PoseStack g, CallbackInfo ci) {
		Backgrounds.render(Gfx.of(g));
		GuiComponent.fill(g, 0, 0, width, height, 0x780B0D11);
		ci.cancel();
	}

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void kromvia$inWorld(PoseStack g, CallbackInfo ci) {
		if (Minecraft.getInstance().level == null) return;
		GuiComponent.fill(g, 0, 0, width, height, 0x780B0D11);
		ci.cancel();
	}
}
