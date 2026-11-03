package com.kromvia.client.mixin;

import com.kromvia.client.Rooms;
import com.kromvia.client.cosmetic.Effects;
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
		Effects.tick((Minecraft) (Object) this);
		Hud.tick((Minecraft) (Object) this);
		Rooms.tick((Minecraft) (Object) this);
	}
}
