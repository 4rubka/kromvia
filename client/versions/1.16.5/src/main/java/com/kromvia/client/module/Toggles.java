package com.kromvia.client.module;

import com.kromvia.client.compat.Mc;
import com.mojang.blaze3d.platform.InputConstants;
import com.kromvia.client.mixin.OptionInstanceAccessor;
import com.kromvia.client.module.setting.BoolSetting;
import com.kromvia.client.module.setting.SliderSetting;
import net.minecraft.world.item.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class Toggles {
	private Toggles() {}

	private abstract static class Utility extends Module {
		Utility(String id, String name, boolean on) {
			super(id, name, Category.UTILITY, Corner.TOP_LEFT, on);
		}

		@Override
		public boolean hud() {
			return false;
		}
	}

	public static final class Zoom extends Utility {
		private static float current = 1;
		private static float wheel = 1;
		private static boolean held;
		private static Zoom instance;
		final SliderSetting power = add(new SliderSetting("power", "Zoom", 4, 2, 10, 0.5F, "x"));
		final BoolSetting smooth = add(new BoolSetting("smooth", "Smooth", true));

		public Zoom() {
			super("zoom", "Zoom (hold C)", true);
			icon(Items.ENDER_EYE);
			instance = this;
		}

		public static boolean scroll(double dy) {
			if (instance == null || !instance.enabled || !held) return false;
			wheel = Mth.clamp(wheel * (dy > 0 ? 1.25F : 0.8F), 0.4F, 5F);
			return true;
		}

		public static float factor() {
			return current;
		}

		@Override
		public void tick(Minecraft mc) {
			held = Mc.screen() == null && Mc.keyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_C);
			if (!held) wheel = 1;
			float target = held ? 1F / (power.value() * wheel) : 1F;
			current = smooth.on() ? Mth.lerp(0.5F, current, target) : target;
			if (Math.abs(current - 1) < 0.01F) current = 1;
		}

		@Override
		public void disable(Minecraft mc) {
			current = 1;
			wheel = 1;
			held = false;
		}
	}

	public static final class Fullbright extends Utility {
		private Double saved;

		public Fullbright() {
			super("fullbright", "Fullbright", false);
			icon(Items.GLOWSTONE);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void tick(Minecraft mc) {
			double gamma = mc.options.gamma;
			if (saved == null) saved = mc.options.gamma;
			mc.options.gamma = 16.0;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void disable(Minecraft mc) {
			if (saved != null) mc.options.gamma = saved;
			saved = null;
		}
	}

	public static final class AutoSprint extends Utility {
		public AutoSprint() {
			super("auto_sprint", "Auto Sprint", false);
			icon(Items.GOLDEN_BOOTS);
		}

		@Override
		public void tick(Minecraft mc) {
			net.minecraft.client.player.LocalPlayer p = mc.player;
			if (p != null && mc.options.keyUp.isDown() && !p.isShiftKeyDown() && !p.isUsingItem() && p.getFoodData().getFoodLevel() > 6) {
				p.setSprinting(true);
			}
		}
	}
}
