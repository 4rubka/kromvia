package com.kromvia.client.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.module.Features;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HudMixin {
	static {
		Features.CROSSHAIR.invert.shown = true;
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void kromvia$renderHud(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
		com.kromvia.client.module.Hud.render(Gfx.of(graphics));
	}

	@Redirect(method = "extractCrosshair", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private void kromvia$crosshair(GuiGraphicsExtractor g, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
		if (Features.CROSSHAIR.enabled) Features.CROSSHAIR.draw(Gfx.of(g), g.guiWidth() / 2, g.guiHeight() / 2, (x1, y1, x2, y2) -> g.fill(RenderPipelines.GUI_INVERT, x1, y1, x2, y2, -1));
		else g.blitSprite(pipeline, sprite, x, y, w, h);
	}

	@Inject(method = "extractTextureOverlay", at = @At("HEAD"), cancellable = true)
	private void kromvia$noPumpkin(GuiGraphicsExtractor g, Identifier texture, float alpha, CallbackInfo ci) {
		if (Features.NO_PUMPKIN.enabled && texture.getPath().contains("pumpkinblur")) ci.cancel();
	}

	@Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
	private void kromvia$hideScoreboard(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
		if (Features.HIDE_SCOREBOARD.enabled) ci.cancel();
	}
}
