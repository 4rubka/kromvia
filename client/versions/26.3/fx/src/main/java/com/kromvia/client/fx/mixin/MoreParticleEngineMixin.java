package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.More;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class MoreParticleEngineMixin {
	@Inject(method = "createParticle", at = @At("RETURN"))
	private void kromvia$tint(ParticleOptions options, double x, double y, double z, double dx, double dy, double dz, CallbackInfoReturnable<Particle> cir) {
		More.tint(options, cir.getReturnValue());
	}
}
