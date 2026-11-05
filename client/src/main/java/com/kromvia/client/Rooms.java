package com.kromvia.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.HttpUtil;

public final class Rooms {
	private static final boolean HOSTING = "1".equals(System.getenv("KROMVIA_ROOM"));
	private static Object tried;
	private static boolean hostNext;

	private Rooms() {}

	public static boolean relay() {
		return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("e4mc");
	}

	public static void hostNext(boolean on) {
		hostNext = on;
	}

	public static void hostNow() {
		hostNext = true;
		tried = null;
	}

	public static void tick(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (!(HOSTING || hostNext) || server == null || mc.player == null || server == tried) return;
		tried = server;
		hostNext = false;
		if (!server.isPublished()) server.publishServer(null, false, HttpUtil.getAvailablePort());
	}
}
