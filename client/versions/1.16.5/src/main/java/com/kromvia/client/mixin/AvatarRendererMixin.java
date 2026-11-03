package com.kromvia.client.mixin;

import com.kromvia.client.cosmetic.CosmeticsLayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public class AvatarRendererMixin {
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;Z)V", at = @At("TAIL"))
	private void kromvia$addCosmetics(EntityRenderDispatcher dispatcher, boolean slim, CallbackInfo ci) {
		PlayerRenderer self = (PlayerRenderer) (Object) this;
		((LivingEntityRendererInvoker) self).kromvia$addLayer(new CosmeticsLayer((PlayerRenderer) self));
	}
}
