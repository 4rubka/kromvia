package com.kromvia.client.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.module.Features;
import com.kromvia.client.module.Hud;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
	static {
		Features.CROSSHAIR.invert.shown = true;
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void kromvia$renderHud(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
		Hud.render(Gfx.of(graphics));
	}

	@Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private void kromvia$crosshair(GuiGraphics g, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
		if (Features.CROSSHAIR.enabled) Features.CROSSHAIR.draw(Gfx.of(g), g.guiWidth() / 2, g.guiHeight() / 2, (x1, y1, x2, y2) -> g.fill(RenderPipelines.GUI_INVERT, x1, y1, x2, y2, -1));
		else g.blitSprite(pipeline, sprite, x, y, w, h);
	}

	@Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
	private void kromvia$noPumpkin(GuiGraphics g, Identifier texture, float alpha, CallbackInfo ci) {
		if (Features.NO_PUMPKIN.enabled && texture.getPath().contains("pumpkinblur")) ci.cancel();
	}

	@Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
	private void kromvia$hideScoreboard(GuiGraphics g, Objective objective, CallbackInfo ci) {
		if (Features.HIDE_SCOREBOARD.enabled) ci.cancel();
	}
}
