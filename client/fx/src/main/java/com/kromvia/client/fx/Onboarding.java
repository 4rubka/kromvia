package com.kromvia.client.fx;

import com.kromvia.client.Accounts;
import com.kromvia.client.Config;
import com.kromvia.client.KromviaClient;
import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.KScreen;
import com.kromvia.client.compat.Mc;
import com.kromvia.client.module.Module;
import com.kromvia.client.module.Toggles;
import com.kromvia.client.ui.KButton;
import com.kromvia.client.ui.UiStyle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

public final class Onboarding extends KScreen {
	private static final Path DONE = FabricLoader.getInstance().getConfigDir().resolve("kromvia-onboarded");
	private static final String[] PAGES = {"Welcome", "Look", "Controls", "HUD", "Visuals", "Mods", "Done"};

	private record Extra(String slug, String modId, String label) {}

	private static final List<Extra> EXTRAS = List.of(
			new Extra("appleskin", "appleskin", "AppleSkin: hunger and saturation on the food bar"),
			new Extra("lambdynamiclights", "lambdynlights", "Dynamic Lights: torches light up in your hand"),
			new Extra("3dskinlayers", "skinlayers3d", "3D Skin Layers: the skin's outer layer in 3D"),
			new Extra("wavey-capes", "waveycapes", "Wavey Capes: capes that move with you"),
			new Extra("bettergrassify", "bettergrass", "BetterGrassify: grass sides match the top"));
	private static final java.util.Set<String> PICKED = new java.util.LinkedHashSet<>();
	private static final List<String> HUD = List.of("fps", "cps", "ping", "coords", "keystrokes", "armor", "effects", "clock", "speed", "memory", "crosshair", "combo");
	private static final int[] SCALES = {0, 1, 2, 3, 4};

	public static final Module AGAIN = new Module("welcome_guide", "Show Welcome Guide", Module.Category.UTILITY, Module.Corner.TOP_LEFT, false) {
		{
			icon(Items.MAP);
		}

		@Override
		public boolean hud() {
			return false;
		}
	};

	private final Screen parent;
	private final Runnable resize;
	private int page;
	private int px, py, pw, ph;

	public Onboarding(Screen parent, Runnable resize) {
		super(Component.literal("Welcome to Kromvia"));
		this.parent = parent;
		this.resize = resize;
	}

	public static boolean needed() {
		return AGAIN.enabled || !Files.exists(DONE);
	}

	private static Module module(String id) {
		return KromviaClient.MODULES.stream().filter(m -> m.id.equals(id)).findFirst().orElse(null);
	}

	private static Module sprint() {
		return KromviaClient.MODULES.stream().filter(m -> m instanceof Toggles.AutoSprint).findFirst().orElse(null);
	}

	@Override
	protected void init() {
		pw = Math.min(width - 40, 360);
		ph = Math.min(height - 30, 240);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		int x = px + 16, w = pw - 32, y = py + 58;

		switch (PAGES[page]) {
			case "Look" -> {
				int bw = (w - 4 * 4) / SCALES.length;
				for (int i = 0; i < SCALES.length; i++) {
					int scale = SCALES[i];
					addRenderableWidget(new KButton(x + i * (bw + 4), y, bw, 20, Component.literal(scale == 0 ? "Auto" : scale + "x"), b -> {
						minecraft.options.guiScale().set(scale);
						minecraft.options.save();
						resize.run();
					}, () -> minecraft.options.guiScale().get() == scale));
				}
			}
			case "Controls" -> {
				toggle(sprint(), "Auto Sprint: sprint whenever you walk forward", x, y, w);
				toggle(module("toggle_sneak"), "Toggle Sneak: press once to sneak", x, y + 24, w);
			}
			case "HUD" -> {
				List<Module> list = HUD.stream().map(Onboarding::module).filter(m -> m != null).toList();
				int cols = 3, bw = (w - (cols - 1) * 4) / cols;
				for (int i = 0; i < list.size(); i++) toggle(list.get(i), list.get(i).name, x + (i % cols) * (bw + 4), y + (i / cols) * 24, bw);
				allNone(x + w, () -> setAll(list, true), () -> setAll(list, false));
			}
			case "Visuals" -> {
				toggle(module("block_outline"), "Coloured block outline", x, y, w);
				String[] blur = {"Off", "Low", "Medium", "High"};
				int bw = (w - 3 * 4) / blur.length;
				for (int i = 0; i < blur.length; i++) {
					int level = i;
					addRenderableWidget(new KButton(x + i * (bw + 4), y + 48, bw, 20, Component.literal(blur[i]), b -> {
						Fx.MOTION_BLUR.enabled = level > 0;
						if (level > 0) Fx.MOTION_BLUR.strength.set(level - 1);
						Config.save();
					}, () -> Fx.MOTION_BLUR.enabled ? Fx.MOTION_BLUR.strength.index() == level - 1 : level == 0));
				}
			}
			case "Mods" -> {
				for (int i = 0; i < EXTRAS.size(); i++) {
					Extra e = EXTRAS.get(i);
					boolean in = FabricLoader.getInstance().isModLoaded(e.modId());
					KButton b = addRenderableWidget(new KButton(x, y + i * 22, w, 20, Component.literal(e.label()), btn -> {
						if (!PICKED.remove(e.slug())) PICKED.add(e.slug());
					}, () -> in || PICKED.contains(e.slug())).toggle());
					b.active = !in && Accounts.sender != null;
				}
				allNone(x + w, () -> {
					for (Extra e : EXTRAS) if (!FabricLoader.getInstance().isModLoaded(e.modId())) PICKED.add(e.slug());
				}, PICKED::clear);
			}
			default -> {}
		}

		int by = py + ph - 28;
		if (page > 0) addRenderableWidget(new KButton(px + 16, by, 90, 20, Component.literal("Back"), b -> go(page - 1)).flat());
		boolean last = page == PAGES.length - 1;
		addRenderableWidget(new KButton(px + pw - 106, by, 90, 20, Component.literal(last ? "Play" : "Next"), b -> {
			if (last) onClose();
			else go(page + 1);
		}));
	}

	private void toggle(Module m, String label, int x, int y, int w) {
		if (m == null) return;
		addRenderableWidget(new KButton(x, y, w, 20, Component.literal(label), b -> {
			m.enabled = !m.enabled;
			if (!m.enabled) m.disable(minecraft);
			Config.save();
		}, () -> m.enabled).toggle());
	}

	private void allNone(int right, Runnable all, Runnable none) {
		int y = py + 40;
		addRenderableWidget(new KButton(right - 80, y, 38, 14, Component.literal("All"), b -> all.run()).flat());
		addRenderableWidget(new KButton(right - 38, y, 38, 14, Component.literal("None"), b -> none.run()).flat());
	}

	private void setAll(List<Module> list, boolean on) {
		for (Module m : list) {
			if (m.enabled && !on) m.disable(minecraft);
			m.enabled = on;
		}
		Config.save();
	}

	private void go(int to) {
		page = to;
		clearWidgets();
		init();
	}

	@Override
	protected void drawBackground(Gfx g, int mouseX, int mouseY, float delta) {
		UiStyle.dim(g, width, height, 1);
		UiStyle.panel(g, px, py, pw, ph);
	}

	@Override
	protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
		super.draw(g, mouseX, mouseY, delta);
		g.drawString(font, PAGES[page], px + 16, py + 14, UiStyle.TEXT, false);
		g.drawString(font, (page + 1) + " / " + PAGES.length, px + pw - 40, py + 14, UiStyle.MUTED, false);
		for (int i = 0; i < PAGES.length; i++) g.fill(px + 16 + i * 14, py + 28, px + 26 + i * 14, py + 30, i <= page ? UiStyle.FRONT : UiStyle.LINE);
		String[] text = switch (PAGES[page]) {
			case "Welcome" -> new String[] {"Kromvia is built into this game.", "A few quick choices and you are in.", "Everything here can be changed later: Right Shift."};
			case "Look" -> new String[] {"Interface size"};
			case "Controls" -> new String[] {"How you move"};
			case "HUD" -> new String[] {"What the HUD shows"};
			case "Visuals" -> new String[] {"", "", "", "Motion blur"};
			case "Mods" -> new String[] {Accounts.sender == null ? "Mods from Modrinth (needs the Kromvia launcher)" : "Mods from Modrinth, installed for your next start"};
			default -> new String[] {"All set.", "Right Shift opens every module in game."};
		};
		int y = PAGES[page].equals("Welcome") || PAGES[page].equals("Done") ? py + 58 : py + 44;
		for (String line : text) {
			g.drawString(font, line, px + 16, y, UiStyle.MUTED, false);
			y += 12;
		}
	}

	@Override
	public void onClose() {
		if (!PICKED.isEmpty() && Accounts.sender != null) {
			com.google.gson.JsonObject msg = new com.google.gson.JsonObject();
			com.google.gson.JsonArray list = new com.google.gson.JsonArray();
			PICKED.forEach(list::add);
			msg.add("install", list);
			Accounts.sender.accept(msg.toString());
			PICKED.clear();
		}
		try {
			Files.writeString(DONE, "");
		} catch (IOException ignored) {
		}
		AGAIN.enabled = false;
		Config.save();
		Mc.setScreen(parent);
	}
}
