package com.kromvia.client.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import com.kromvia.client.compat.DrawnButton;
import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.Mc;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class ModeCards {
	private static final int W = 66, PIC = 37, H = PIC + 14, GAP = 6, TEX_W = 256, TEX_H = 144;
	private static final SelectedGameMode[] MODES = {SelectedGameMode.SURVIVAL, SelectedGameMode.HARDCORE, SelectedGameMode.CREATIVE};
	private static final Map<SelectedGameMode, Identifier> TEXTURES = new EnumMap<>(SelectedGameMode.class);

	private ModeCards() {}

	public static LayoutElement create() {
		LinearLayout row = LinearLayout.horizontal().spacing(GAP);
		for (SelectedGameMode mode : MODES) row.addChild(new Card(mode));
		return row;
	}

	private static WorldCreationUiState state() {
		return Mc.screen() instanceof CreateWorldScreen s ? s.getUiState() : null;
	}

	private static Identifier texture(SelectedGameMode mode) {
		if (TEXTURES.containsKey(mode)) return TEXTURES.get(mode);
		String name = mode.name().toLowerCase(Locale.ROOT);
		Identifier id = null;
		try (InputStream in = ModeCards.class.getResourceAsStream("/kromvia/modes/" + name + ".png")) {
			if (in != null) {
				id = Identifier.fromNamespaceAndPath("kromvia", "mode/" + name);
				Identifier key = id;
				Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(key::toString, NativeImage.read(in)));
			}
		} catch (IOException ignored) {
			id = null;
		}
		TEXTURES.put(mode, id);
		return id;
	}

	private static final class Card extends DrawnButton {
		private final SelectedGameMode mode;

		Card(SelectedGameMode mode) {
			super(0, 0, W, H, mode.displayName, b -> {});
			this.mode = mode;
			setTooltip(Tooltip.create(mode.getInfo()));
		}

		@Override
		protected void pressed() {
			WorldCreationUiState s = state();
			if (s != null && !s.isDebug()) s.setGameMode(mode);
		}

		@Override
		protected void drawContents(Gfx g, int mouseX, int mouseY, float delta) {
			WorldCreationUiState s = state();
			active = s == null || !s.isDebug();
			boolean picked = s != null && s.getGameMode() == mode;
			int x = getX(), y = getY();
			int edge = picked ? UiStyle.FRONT : isHoveredOrFocused() && active ? UiStyle.MUTED : UiStyle.LINE;
			g.fill(x, y, x + W, y + H, edge);
			g.fill(x + 1, y + 1, x + W - 1, y + H - 1, UiStyle.CARD);
			Identifier tex = texture(mode);
			if (tex != null) g.blit(tex, x + 1, y + 1, 0, 0, W - 2, PIC, TEX_W, TEX_H, TEX_W, TEX_H, active ? 0xFFFFFFFF : 0xFF5A5A5A);
			g.fill(x + W - 10, y + 3, x + W - 3, y + 12, 0xC0000000);
			g.drawString(Minecraft.getInstance().font, "?", x + W - 8, y + 4, UiStyle.TEXT, false);
			g.drawCenteredString(Minecraft.getInstance().font, mode.displayName, x + W / 2, y + PIC + 3, picked ? UiStyle.TEXT : UiStyle.MUTED);
		}
	}
}
