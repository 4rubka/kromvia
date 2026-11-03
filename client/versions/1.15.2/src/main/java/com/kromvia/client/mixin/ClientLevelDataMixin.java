package com.kromvia.client.mixin;

import com.kromvia.client.module.Features;
import net.minecraft.world.level.dimension.Dimension;
import net.minecraft.world.level.dimension.NormalDimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(NormalDimension.class)
public abstract class ClientLevelDataMixin extends Dimension {
	private ClientLevelDataMixin() {
		super(null, null, 0);
	}

	@ModifyVariable(method = "getTimeOfDay", at = @At("HEAD"), argsOnly = true)
	private long kromvia$time(long time) {
		return Features.TIME.enabled && level.isClientSide ? Features.fixedTime() : time;
	}
}
