package com.kromvia.client.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.module.Features;
import com.kromvia.client.module.Hud;
import net.minecraft.client.gui.Gui;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void kromvia$renderHud(PoseStack graphics, float delta, CallbackInfo ci) {
		Hud.render(Gfx.of(graphics));
	}

	@Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/Gui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"))
	private void kromvia$crosshair(Gui self, PoseStack g, int x, int y, int u, int v, int w, int h) {
		if (Features.CROSSHAIR.enabled) Features.CROSSHAIR.draw(Gfx.of(g), Gfx.of(g).guiWidth() / 2, Gfx.of(g).guiHeight() / 2);
		else self.blit(g, x, y, u, v, w, h);
	}

	@Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
	private void kromvia$noPumpkin(ResourceLocation texture, float alpha, CallbackInfo ci) {
		if (Features.NO_PUMPKIN.enabled && texture.getPath().contains("pumpkinblur")) ci.cancel();
	}

	@Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
	private void kromvia$hideScoreboard(PoseStack g, Objective objective, CallbackInfo ci) {
		if (Features.HIDE_SCOREBOARD.enabled) ci.cancel();
	}
}
