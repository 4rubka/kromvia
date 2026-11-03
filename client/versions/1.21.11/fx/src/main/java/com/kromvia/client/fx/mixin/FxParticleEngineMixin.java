package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import net.minecraft.client.particle.HugeExplosionParticle;
import net.minecraft.client.particle.HugeExplosionSeedParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class FxParticleEngineMixin {
	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void kromvia$filter(Particle p, CallbackInfo ci) {
		if (Tweaks.dropParticle(p instanceof TerrainParticle, p instanceof HugeExplosionParticle || p instanceof HugeExplosionSeedParticle)) ci.cancel();
	}
}
