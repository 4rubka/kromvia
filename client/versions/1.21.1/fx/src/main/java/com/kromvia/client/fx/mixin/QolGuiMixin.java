package com.kromvia.client.fx.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.fx.Qol;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class QolGuiMixin {
	@Inject(method = "renderEffects", at = @At("TAIL"))
	private void kromvia$timers(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		Qol.effectTimers(Gfx.of(g), Minecraft.getInstance());
	}

	@Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
	private static void kromvia$armor(GuiGraphics g, Player player, int yLineBase, int healthRows, int rowHeight, int xLeft, CallbackInfo ci) {
		if (!Qol.detailedArmor()) return;
		ci.cancel();
		int y = yLineBase - (healthRows - 1) * rowHeight - 10;
		Qol.drawArmor(Gfx.of(g), Minecraft.getInstance(), player, xLeft, y, (sprite, x, sy, color) -> {
			RenderSystem.setShaderColor((color >> 16 & 0xFF) / 255F, (color >> 8 & 0xFF) / 255F, (color & 0xFF) / 255F, (color >>> 24) / 255F);
			g.blitSprite(sprite, x, sy, 9, 9);
			RenderSystem.setShaderColor(1, 1, 1, 1);
		});
	}
}
