package com.kromvia.client.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.Category.class)
public class KeyCategoryMixin {
	@Inject(method = "label", at = @At("HEAD"), cancellable = true)
	private void kromvia$label(CallbackInfoReturnable<Component> cir) {
		if ("kromvia".equals(((KeyMapping.Category) (Object) this).id().getNamespace())) cir.setReturnValue(Component.literal("Kromvia"));
	}
}
