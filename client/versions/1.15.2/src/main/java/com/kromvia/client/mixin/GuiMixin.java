package com.kromvia.client.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.module.Features;
import com.kromvia.client.module.Hud;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void kromvia$renderHud(float delta, CallbackInfo ci) {
		Hud.render(Gfx.of());
	}

	@Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/Gui;blit(IIIIII)V"))
	private void kromvia$crosshair(Gui self, int x, int y, int u, int v, int w, int h) {
		if (Features.CROSSHAIR.enabled) Features.CROSSHAIR.draw(Gfx.of(), Gfx.of().guiWidth() / 2, Gfx.of().guiHeight() / 2);
		else self.blit(x, y, u, v, w, h);
	}

	@Inject(method = "renderPumpkin", at = @At("HEAD"), cancellable = true)
	private void kromvia$noPumpkin(CallbackInfo ci) {
		if (Features.NO_PUMPKIN.enabled) ci.cancel();
	}

	@Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
	private void kromvia$hideScoreboard(Objective objective, CallbackInfo ci) {
		if (Features.HIDE_SCOREBOARD.enabled) ci.cancel();
	}
}
