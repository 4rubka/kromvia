package com.kromvia.client.mixin;

import com.kromvia.client.Rooms;
import com.kromvia.client.module.Freecam;
import com.kromvia.client.module.Hud;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void kromvia$tickEffects(CallbackInfo ci) {
		Hud.tick((Minecraft) (Object) this);
		Rooms.tick((Minecraft) (Object) this);
	}

	@Inject(method = "pick", at = @At("TAIL"))
	private void kromvia$freecamAim(float partialTicks, CallbackInfo ci) {
		Freecam.aim((Minecraft) (Object) this);
	}
}
