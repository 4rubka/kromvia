package com.kromvia.client.fx.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.fx.Qol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class QolGuiMixin {
	@Inject(method = "renderEffects", at = @At("TAIL"))
	private void kromvia$timers(PoseStack g, CallbackInfo ci) {
		Qol.effectTimers(Gfx.of(g), Minecraft.getInstance());
	}
}
