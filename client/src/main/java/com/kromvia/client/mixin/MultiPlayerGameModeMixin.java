package com.kromvia.client.mixin;

import com.kromvia.client.module.Extras;
import com.kromvia.client.module.Features;
import com.kromvia.client.module.Visuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void kromvia$attack(Player player, Entity target, CallbackInfo ci) {
		Features.onAttack(Minecraft.getInstance(), target);
		Visuals.onAttack(Minecraft.getInstance(), target);
		Extras.onAttack(Minecraft.getInstance(), target);
	}
}
