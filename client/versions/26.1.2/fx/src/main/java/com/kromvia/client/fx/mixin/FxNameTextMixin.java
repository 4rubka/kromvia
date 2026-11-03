package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SubmitNodeCollection.class)
public class FxNameTextMixin {
	@ModifyVariable(method = "submitNameTag", at = @At("HEAD"), argsOnly = true)
	private Component kromvia$hideName(Component name) {
		return Tweaks.hideName(name);
	}
}
