package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Fx;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class FxFirstPersonHandsAndItemsRendererMixin {
	@Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER, ordinal = 0))
	private void kromvia$handPosition(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, float partial, float xRot, InteractionHand hand,
			float attack, ItemStack stack, float equip, PoseStack pose, SubmitNodeCollector out, int light, CallbackInfo ci) {
		if (Fx.HAND.enabled) Fx.HAND.apply(pose);
		com.kromvia.client.fx.Tweaks.OVERLAYS.lowerShield(pose, stack);
	}
}
