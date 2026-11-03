package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.OutfitLayer;
import com.kromvia.client.fx.Players;
import com.kromvia.client.mixin.LivingEntityRendererInvoker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class FxAvatarRendererMixin {
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(method = "<init>", at = @At("TAIL"))
	private void kromvia$addOutfit(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
		PlayerRenderer self = (PlayerRenderer) (Object) this;
		((LivingEntityRendererInvoker) self).kromvia$addLayer(new OutfitLayer(self));
	}

	@Unique
	private boolean kromvia$badging;

	@Shadow
	protected abstract void renderNameTag(AbstractClientPlayer player, Component name, PoseStack pose, MultiBufferSource buffers, int light, float partial);

	@Inject(method = "renderNameTag(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V", at = @At("HEAD"), cancellable = true)
	private void kromvia$badge(AbstractClientPlayer player, Component name, PoseStack pose, MultiBufferSource buffers, int light, float partial, CallbackInfo ci) {
		if (kromvia$badging) return;
		Component badged = Players.badged(player.getGameProfile().getName(), name);
		if (badged == name) return;
		kromvia$badging = true;
		try {
			renderNameTag(player, badged, pose, buffers, light, partial);
		} finally {
			kromvia$badging = false;
		}
		ci.cancel();
	}
}
