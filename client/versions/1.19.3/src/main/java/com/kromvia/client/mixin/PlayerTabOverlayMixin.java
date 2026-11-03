package com.kromvia.client.mixin;

import com.kromvia.client.module.Extras;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
	@Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true)
	private void kromvia$ping(PoseStack g, int width, int x, int y, PlayerInfo info, CallbackInfo ci) {
		if (!Extras.TAB_PING.enabled) return;
		ci.cancel();
		int ms = info.getLatency();
		if (ms < 0) return;
		String s = String.valueOf(Math.min(ms, 9999));
		var font = Minecraft.getInstance().font;
		g.pushPose();
		g.translate(x + width - 1, y + 1.5F, 0);
		g.scale(0.75F, 0.75F, 1);
		font.drawShadow(g, s, -font.width(s), 0, Extras.TAB_PING.color(ms));
		g.popPose();
	}
}
