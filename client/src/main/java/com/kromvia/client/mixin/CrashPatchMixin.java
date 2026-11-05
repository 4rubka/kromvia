package com.kromvia.client.mixin;

import com.kromvia.client.module.Features;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class CrashPatchMixin {
	@Shadow
	private void runTick(boolean renderLevel) {
		throw new AssertionError();
	}

	@Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runTick(Z)V"))
	private void kromvia$survive(Minecraft mc, boolean renderLevel) {
		try {
			runTick(renderLevel);
		} catch (OutOfMemoryError e) {
			throw e;
		} catch (RuntimeException | Error e) {
			if (!Features.survive(e)) throw e;
		}
	}
}
