package com.kromvia.client.fx;

import net.minecraft.client.Minecraft;

final class Say {
	static void send(Minecraft mc, String text) {
		if (text.startsWith("/")) mc.player.commandSigned(text.substring(1), null);
		else mc.player.chatSigned(text, null);
	}
}
