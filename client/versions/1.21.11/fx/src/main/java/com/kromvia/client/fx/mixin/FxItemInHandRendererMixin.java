package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Fx;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class FxItemInHandRendererMixin {
	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"))
	private float kromvia$noCooldownDip(LocalPlayer player, float partial) {
		return Fx.OLD_ANIMATIONS.enabled ? 1 : player.getItemSwapScale(partial);
	}

	@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER, ordinal = 0))
	private void kromvia$handPosition(AbstractClientPlayer player, float partial, float pitch, InteractionHand hand, float swing, ItemStack stack,
			float equip, PoseStack pose, SubmitNodeCollector out, int light, CallbackInfo ci) {
		if (Fx.HAND.enabled) Fx.HAND.apply(pose);
		com.kromvia.client.fx.Tweaks.OVERLAYS.lowerShield(pose, stack);
	}
}
