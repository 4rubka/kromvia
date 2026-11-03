package com.kromvia.client.mixin;

import com.kromvia.client.module.Extras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Inject(method = "handleEntityEvent", at = @At("TAIL"))
	private void kromvia$totem(ClientboundEntityEventPacket packet, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (packet.getEventId() != 35 || mc.level == null) return;
		Entity e = packet.getEntity(mc.level);
		if (e != null) Extras.onTotem(mc, e);
	}

	@Inject(method = "handleSetTime", at = @At("TAIL"))
	private void kromvia$tps(ClientboundSetTimePacket packet, CallbackInfo ci) {
		Extras.onServerTime(packet.gameTime());
	}
}
