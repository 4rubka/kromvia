package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class FxScoreboardMixin {
	static {
		Tweaks.SCOREBOARD.shown = true;
		Tweaks.VANILLA_HUD.shown = true;
	}

	@WrapOperation(method = "extractScoreboardSidebar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V"))
	private void kromvia$place(Hud self, GuiGraphicsExtractor g, Objective objective, Operation<Void> op) {
		Tweaks.Scoreboard s = Tweaks.SCOREBOARD;
		if (!s.enabled) {
			op.call(self, g, objective);
			return;
		}
		float x = g.guiWidth(), y = g.guiHeight() / 2f, k = s.scale.value();
		g.pose().pushMatrix();
		g.pose().translate(x, y + s.offset.value());
		g.pose().scale(k, k);
		g.pose().translate(-x, -y);
		op.call(self, g, objective);
		g.pose().popMatrix();
	}

	@Redirect(method = "displayScoreboardSidebar", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Objective;numberFormatOrDefault(Lnet/minecraft/network/chat/numbers/NumberFormat;)Lnet/minecraft/network/chat/numbers/NumberFormat;"))
	private NumberFormat kromvia$numbers(Objective objective, NumberFormat fallback) {
		Tweaks.Scoreboard s = Tweaks.SCOREBOARD;
		return s.enabled && !s.numbers.on() ? BlankFormat.INSTANCE : objective.numberFormatOrDefault(fallback);
	}

	@ModifyArg(method = "displayScoreboardSidebar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"), index = 4)
	private int kromvia$background(int argb) {
		return Tweaks.SCOREBOARD.background(argb);
	}

	@Unique
	private static void kromvia$push(GuiGraphicsExtractor g, float x, float y, float k, float dy) {
		g.pose().pushMatrix();
		g.pose().translate(x, y + dy);
		g.pose().scale(k, k);
		g.pose().translate(-x, -y);
	}

	@Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
	private void kromvia$titleIn(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
		Tweaks.VanillaHud h = Tweaks.VANILLA_HUD;
		if (!h.enabled) return;
		if (!h.title.on()) {
			ci.cancel();
			return;
		}
		kromvia$push(g, g.guiWidth() / 2f, g.guiHeight() / 2f, h.titleScale.value(), 0);
	}

	@Inject(method = "extractTitle", at = @At("RETURN"))
	private void kromvia$titleOut(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
		if (Tweaks.VANILLA_HUD.enabled && Tweaks.VANILLA_HUD.title.on()) g.pose().popMatrix();
	}

	@Inject(method = "extractOverlayMessage", at = @At("HEAD"), cancellable = true)
	private void kromvia$actionIn(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
		Tweaks.VanillaHud h = Tweaks.VANILLA_HUD;
		if (!h.enabled) return;
		if (!h.actionbar.on()) {
			ci.cancel();
			return;
		}
		kromvia$push(g, g.guiWidth() / 2f, g.guiHeight() - 68, h.actionbarScale.value(), h.actionbarOffset.value());
	}

	@Inject(method = "extractOverlayMessage", at = @At("RETURN"))
	private void kromvia$actionOut(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
		if (Tweaks.VANILLA_HUD.enabled && Tweaks.VANILLA_HUD.actionbar.on()) g.pose().popMatrix();
	}

	@Inject(method = "extractBossOverlay", at = @At("HEAD"), cancellable = true)
	private void kromvia$bossIn(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
		Tweaks.VanillaHud h = Tweaks.VANILLA_HUD;
		if (!h.enabled) return;
		if (!h.bossbar.on()) {
			ci.cancel();
			return;
		}
		kromvia$push(g, g.guiWidth() / 2f, 0, h.bossbarScale.value(), 0);
	}

	@Inject(method = "extractBossOverlay", at = @At("RETURN"))
	private void kromvia$bossOut(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
		if (Tweaks.VANILLA_HUD.enabled && Tweaks.VANILLA_HUD.bossbar.on()) g.pose().popMatrix();
	}
}
