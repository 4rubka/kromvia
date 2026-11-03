package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.OutfitLayer;
import com.kromvia.client.fx.Players;
import com.kromvia.client.mixin.LivingEntityRendererInvoker;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public class FxAvatarRendererMixin {
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(method = "<init>", at = @At("TAIL"))
	private void kromvia$addOutfit(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
		PlayerRenderer self = (PlayerRenderer) (Object) this;
		((LivingEntityRendererInvoker) self).kromvia$addLayer(new OutfitLayer(self));
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V", at = @At("TAIL"))
	private void kromvia$badge(AbstractClientPlayer player, PlayerRenderState state, float partial, CallbackInfo ci) {
		state.nameTag = Players.badged(player.getGameProfile().getName(), state.nameTag);
	}
}
