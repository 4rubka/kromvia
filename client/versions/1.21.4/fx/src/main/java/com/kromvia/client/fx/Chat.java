package com.kromvia.client.fx;

import com.mojang.blaze3d.platform.NativeImage;
import java.net.URI;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

final class Chat {
	private Chat() {}

	static ClickEvent run(String command) {
		return new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
	}

	static ClickEvent copy(String text) {
		return new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text);
	}

	static ClickEvent openFile(String path) {
		return new ClickEvent(ClickEvent.Action.OPEN_FILE, path);
	}

	static HoverEvent hover(Component text) {
		return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
	}

	static net.minecraft.client.gui.screens.Screen chatWith(String text) {
		return new net.minecraft.client.gui.screens.ChatScreen(text);
	}

	static String command(ClickEvent event) {
		return event != null && event.getAction() == ClickEvent.Action.RUN_COMMAND ? event.getValue() : null;
	}

	static String filePath(ClickEvent event) {
		return event != null && event.getAction() == ClickEvent.Action.OPEN_FILE ? event.getValue() : null;
	}

	static URI url(ClickEvent event) {
		if (event == null || event.getAction() != ClickEvent.Action.OPEN_URL) return null;
		try {
			return new URI(event.getValue());
		} catch (java.net.URISyntaxException e) {
			return null;
		}
	}

	static DynamicTexture texture(String name, NativeImage image) {
		return new DynamicTexture(image);
	}
}
