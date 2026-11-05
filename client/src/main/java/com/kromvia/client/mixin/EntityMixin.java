package com.kromvia.client.mixin;

import com.kromvia.client.module.Features;
import com.kromvia.client.module.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void kromvia$freelook(double dx, double dy, CallbackInfo ci) {
		if (Freecam.active() && (Object) this == Minecraft.getInstance().player) {
			Freecam.turn(dx, dy);
			ci.cancel();
			return;
		}
		if (Features.Freelook.looking() && (Object) this == Minecraft.getInstance().player) {
			Features.Freelook.turn(dx, dy);
			ci.cancel();
		}
	}
}
