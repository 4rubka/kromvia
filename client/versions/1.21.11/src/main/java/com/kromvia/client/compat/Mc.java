package com.kromvia.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class Mc {
	private Mc() {}

	private static Minecraft mc() {
		return Minecraft.getInstance();
	}

	public static Screen screen() {
		return mc().screen;
	}

	public static void setScreen(Screen screen) {
		mc().setScreen(screen);
	}

	public static void leaveWorld(Screen next) {
		if (mc().level != null) mc().disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
		setScreen(next);
	}

	public static boolean hudHidden() {
		return mc().options.hideGui;
	}

	public static void actionbar(Component message) {
		mc().gui.setOverlayMessage(message, false);
	}

	public static void say(Component message) {
		mc().gui.getChat().addMessage(message);
	}

	public static long dayTime(Level level) {
		return level.getDayTime();
	}

	public static boolean keyDown(int key) {
		return InputConstants.isKeyDown(mc().getWindow(), key);
	}

	private static KeyMapping.Category binds;

	public static KeyMapping bind(String name, int key) {
		if (binds == null) binds = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("kromvia", "binds"));
		return new KeyMapping(name, key, binds);
	}

	public static Item redDye() {
		return Items.RED_DYE;
	}

	public static EntityType<?> playerType() {
		return EntityType.PLAYER;
	}

	public static Supplier<PlayerSkin> ownSkin() {
		return mc().getSkinManager().createLookup(mc().getGameProfile(), false);
	}

	public static void fullBright(EntityRenderState state) {
		state.lightCoords = 0xF000F0;
	}
}
