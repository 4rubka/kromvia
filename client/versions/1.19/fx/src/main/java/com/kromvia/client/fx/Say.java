package com.kromvia.client.fx;

import net.minecraft.client.Minecraft;

final class Say {
	static void send(Minecraft mc, String text) {
		if (text.startsWith("/")) mc.player.command(text.substring(1), null);
		else mc.player.chat(text, null);
	}
}
