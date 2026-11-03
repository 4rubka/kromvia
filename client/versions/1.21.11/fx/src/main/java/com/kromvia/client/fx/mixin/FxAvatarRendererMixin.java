package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.OutfitLayer;
import com.kromvia.client.fx.Players;
import com.kromvia.client.mixin.LivingEntityRendererInvoker;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class FxAvatarRendererMixin {
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(method = "<init>", at = @At("TAIL"))
	private void kromvia$addOutfit(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
		AvatarRenderer self = (AvatarRenderer) (Object) this;
		((LivingEntityRendererInvoker) self).kromvia$addLayer(new OutfitLayer((AvatarRenderer) self));
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void kromvia$badge(Avatar avatar, AvatarRenderState state, float partial, CallbackInfo ci) {
		if (avatar instanceof Player p) state.nameTag = Players.badged(p.getGameProfile().name(), state.nameTag);
	}
}
