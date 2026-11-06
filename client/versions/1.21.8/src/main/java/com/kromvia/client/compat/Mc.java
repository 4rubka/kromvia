package com.kromvia.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
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

	public static void leaveWorld(Screen next) {
		Minecraft mc = mc();
		if (mc.level == null) {
			setScreen(next);
			return;
		}
		mc.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
		mc.disconnect(next, false);
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
		return InputConstants.isKeyDown(mc().getWindow().getWindow(), key);
	}

	private static final String BINDS = "Kromvia";
	private static boolean sorted;

	@SuppressWarnings("unchecked")
	public static KeyMapping bind(String name, int key) {
		if (!sorted) {
			sorted = true;
			try {
				for (java.lang.reflect.Field f : KeyMapping.class.getDeclaredFields()) {
					if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getType() != java.util.Map.class) continue;
					f.setAccessible(true);
					java.util.Map<Object, Object> order = (java.util.Map<Object, Object>) f.get(null);
					if (order.get("key.categories.movement") instanceof Integer) order.put(BINDS, order.size() + 1);
				}
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		}
		return new KeyMapping(name, key, BINDS);
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
