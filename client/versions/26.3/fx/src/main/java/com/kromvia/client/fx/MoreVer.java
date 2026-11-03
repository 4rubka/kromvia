package com.kromvia.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;

final class MoreVer {
	private MoreVer() {}

	static ChatComponent chat(Minecraft mc) {
		return mc.gui.hud.getChat();
	}
}
