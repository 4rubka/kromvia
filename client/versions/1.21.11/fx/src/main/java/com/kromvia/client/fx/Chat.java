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
		return new ClickEvent.RunCommand(command);
	}

	static ClickEvent copy(String text) {
		return new ClickEvent.CopyToClipboard(text);
	}

	static ClickEvent openFile(String path) {
		return new ClickEvent.OpenFile(path);
	}

	static HoverEvent hover(Component text) {
		return new HoverEvent.ShowText(text);
	}

	static net.minecraft.client.gui.screens.Screen chatWith(String text) {
		return new net.minecraft.client.gui.screens.ChatScreen(text, false);
	}

	static String command(ClickEvent event) {
		return event instanceof ClickEvent.RunCommand run ? run.command() : null;
	}

	static String filePath(ClickEvent event) {
		return event instanceof ClickEvent.OpenFile open ? open.path() : null;
	}

	static URI url(ClickEvent event) {
		return event instanceof ClickEvent.OpenUrl open ? open.uri() : null;
	}

	static DynamicTexture texture(String name, NativeImage image) {
		return new DynamicTexture(() -> name, image);
	}
}
