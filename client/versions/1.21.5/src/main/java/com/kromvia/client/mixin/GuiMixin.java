package com.kromvia.client.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.module.Features;
import com.kromvia.client.module.Hud;
import java.util.function.Function;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
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
	private void kromvia$renderHud(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
		Hud.render(Gfx.of(graphics));
	}

	@Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V"))
	private void kromvia$crosshair(GuiGraphics g, Function<ResourceLocation, RenderType> type, ResourceLocation sprite, int x, int y, int w, int h) {
		if (Features.CROSSHAIR.enabled) Features.CROSSHAIR.draw(Gfx.of(g), g.guiWidth() / 2, g.guiHeight() / 2);
		else g.blitSprite(type, sprite, x, y, w, h);
	}

	@Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
	private void kromvia$noPumpkin(GuiGraphics g, ResourceLocation texture, float alpha, CallbackInfo ci) {
		if (Features.NO_PUMPKIN.enabled && texture.getPath().contains("pumpkinblur")) ci.cancel();
	}

	@Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
	private void kromvia$hideScoreboard(GuiGraphics g, Objective objective, CallbackInfo ci) {
		if (Features.HIDE_SCOREBOARD.enabled) ci.cancel();
	}
}
