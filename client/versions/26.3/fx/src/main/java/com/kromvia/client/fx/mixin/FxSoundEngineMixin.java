package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class FxSoundEngineMixin {
	@Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
	private void kromvia$volume(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(Tweaks.volume(sound, cir.getReturnValue()));
	}
}
