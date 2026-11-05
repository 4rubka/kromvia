package com.kromvia.client.cosmetic;

import java.util.List;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import com.kromvia.client.compat.PlayerRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public final class Cosmetic {
	private final String id;
	private final String name;
	private final Slot slot;
	private final int defaultColor;
	private final List<Piece> pieces;

	public Cosmetic(String id, String name, Slot slot, int defaultColor, List<Piece> pieces) {
		this.id = id;
		this.name = name;
		this.slot = slot;
		this.defaultColor = defaultColor;
		this.pieces = pieces;
	}

	public String id() {
		return id;
	}

	public String name() {
		return name;
	}

	public Slot slot() {
		return slot;
	}

	public int defaultColor() {
		return defaultColor;
	}

	public List<Piece> pieces() {
		return pieces;
	}

	public enum Slot { HEAD, FACE, NECK, BACK, WEAPON, BODY, EFFECT }

	public enum Anchor { HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG }

	@FunctionalInterface
	public interface Animator {
		void pose(ModelPart part, PlayerRenderState state);
	}

	public static final class Piece {
		final ModelPart part;
		final Anchor anchor;
		final int color;
		final boolean glow;
		final Animator animator;
		private NativeImage pending;
		private final ResourceLocation texture;

		Piece(String name, Shapes.Built built, Anchor anchor, int color, boolean glow, Animator animator) {
			this.part = built.part();
			this.pending = built.image();
			this.texture = new ResourceLocation("kromvia", "cosmetic/" + name);
			this.anchor = anchor;
			this.color = color;
			this.glow = glow;
			this.animator = animator;
		}

		ResourceLocation texture() {
			if (pending != null) {
				Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture( pending));
				pending = null;
			}
			return texture;
		}
	}
}
