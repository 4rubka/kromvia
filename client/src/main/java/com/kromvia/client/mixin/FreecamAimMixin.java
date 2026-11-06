package com.kromvia.client.mixin;

import com.kromvia.client.module.Freecam;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class FreecamAimMixin {
	@Inject(method = "handleKeybinds", at = @At("HEAD"))
	private void kromvia$freecamAim(CallbackInfo ci) {
		Freecam.aim((Minecraft) (Object) this);
	}
}
