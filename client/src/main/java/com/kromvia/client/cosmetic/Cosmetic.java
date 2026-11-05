package com.kromvia.client.cosmetic;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public record Cosmetic(String id, String name, Slot slot, int defaultColor, List<Piece> pieces) {
	public enum Slot { HEAD, FACE, NECK, BACK, WEAPON, BODY, EFFECT }

	public enum Anchor { HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG }

	@FunctionalInterface
	public interface Animator {
		void pose(ModelPart part, AvatarRenderState state);
	}

	public static final class Piece {
		static final int FRAME_MS = 50;
		final ModelPart part;
		final Anchor anchor;
		final int color;
		final boolean glow;
		final Animator animator;
		private Supplier<NativeImage[]> pending;
		private final Identifier[] textures;

		Piece(String name, Shapes.Built built, Anchor anchor, int color, boolean glow, Animator animator) {
			this.part = built.part();
			this.pending = built.images();
			this.textures = new Identifier[built.frames()];
			for (int i = 0; i < textures.length; i++) textures[i] = Identifier.fromNamespaceAndPath("kromvia", "cosmetic/" + name + (i == 0 ? "" : "_" + i));
			this.anchor = anchor;
			this.color = color;
			this.glow = glow;
			this.animator = animator;
		}

		Identifier texture() {
			if (pending != null) {
				NativeImage[] images = pending.get();
				for (int i = 0; i < textures.length; i++) {
					Identifier texture = textures[i];
					Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(texture::toString, images[i]));
				}
				pending = null;
			}
			return textures[(int) (System.currentTimeMillis() / FRAME_MS % textures.length)];
		}
	}
}
