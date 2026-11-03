package com.kromvia.client.fx;

import net.minecraft.client.Minecraft;

final class Say {
	static void send(Minecraft mc, String text) {
		if (text.startsWith("/")) mc.player.connection.sendCommand(text.substring(1));
		else mc.player.connection.sendChat(text);
	}
}
