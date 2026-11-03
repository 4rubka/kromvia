package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class QolLivingEntityMixin {
	@Inject(method = "handleDamageEvent", at = @At("HEAD"))
	private void kromvia$hurtBy(DamageSource source, CallbackInfo ci) {
		Qol.hurt((LivingEntity) (Object) this, source);
	}
}
