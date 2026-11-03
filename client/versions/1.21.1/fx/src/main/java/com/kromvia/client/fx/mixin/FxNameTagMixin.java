package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public class FxNameTagMixin {
	@Redirect(method = "renderNameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getBackgroundOpacity(F)F"))
	private float kromvia$background(Options options, float vanilla) {
		return Tweaks.NAMETAGS.enabled ? Tweaks.NAMETAGS.background() : options.getBackgroundOpacity(vanilla);
	}

	@Redirect(method = "renderNameTag", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
	private void kromvia$scale(PoseStack pose, float x, float y, float z) {
		float s = Tweaks.NAMETAGS.enabled ? Tweaks.NAMETAGS.scale.value() : 1;
		pose.scale(x * s, y * s, z * s);
	}
}
