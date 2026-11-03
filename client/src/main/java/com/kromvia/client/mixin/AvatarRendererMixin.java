package com.kromvia.client.mixin;

import com.kromvia.client.cosmetic.CosmeticsLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(method = "<init>", at = @At("TAIL"))
	private void kromvia$addCosmetics(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
		AvatarRenderer self = (AvatarRenderer) (Object) this;
		((LivingEntityRendererInvoker) self).kromvia$addLayer(new CosmeticsLayer((AvatarRenderer) self));
	}
}
