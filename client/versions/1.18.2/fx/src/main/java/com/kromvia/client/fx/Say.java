package com.kromvia.client.fx;

import net.minecraft.client.Minecraft;

final class Say {
	static void send(Minecraft mc, String text) {
		mc.player.chat(text);
	}
}
