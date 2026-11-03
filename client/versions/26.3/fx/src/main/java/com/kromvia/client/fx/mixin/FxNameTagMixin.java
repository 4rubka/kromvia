package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SubmitNodeCollection.class)
public class FxNameTagMixin {
	@ModifyVariable(method = "submitNameTag", at = @At("HEAD"), argsOnly = true)
	private Component kromvia$hideName(Component name) {
		return Tweaks.hideName(name);
	}

	@Redirect(method = "submitNameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/OptionsRenderState;getBackgroundOpacity(F)F"))
	private float kromvia$background(OptionsRenderState options, float vanilla) {
		return Tweaks.NAMETAGS.enabled ? Tweaks.NAMETAGS.background() : options.getBackgroundOpacity(vanilla);
	}

	@Redirect(method = "submitNameTag", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
	private void kromvia$scale(PoseStack pose, float x, float y, float z) {
		float s = Tweaks.NAMETAGS.enabled ? Tweaks.NAMETAGS.scale.value() : 1;
		pose.scale(x * s, y * s, z * s);
	}
}
