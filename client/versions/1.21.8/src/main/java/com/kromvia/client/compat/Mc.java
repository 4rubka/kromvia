package com.kromvia.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
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

	public static boolean hudHidden() {
		return mc().options.hideGui;
	}

	public static void say(Component message) {
		mc().gui.getChat().addMessage(message);
	}

	public static long dayTime(Level level) {
		return level.getDayTime();
	}

	public static boolean keyDown(int key) {
		return InputConstants.isKeyDown(mc().getWindow().getWindow(), key);
	}

	public static Item redDye() {
		return Items.RED_DYE;
	}

	public static EntityType<?> playerType() {
		return EntityType.PLAYER;
	}

	public static Supplier<PlayerSkin> ownSkin() {
		return mc().getSkinManager().lookupInsecure(mc().getGameProfile());
	}

	public static void fullBright(EntityRenderState state) {}
}
