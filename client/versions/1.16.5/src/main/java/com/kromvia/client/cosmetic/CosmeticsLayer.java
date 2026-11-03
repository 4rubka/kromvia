package com.kromvia.client.cosmetic;

import com.kromvia.client.compat.PlayerLayer;
import com.kromvia.client.cosmetic.Cosmetic.Piece;
import com.kromvia.client.cosmetic.Cosmetic.Slot;
import com.kromvia.client.ui.PlayerPreview;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.IntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import com.kromvia.client.compat.PlayerRenderState;
import net.minecraft.world.item.Items;

public class CosmeticsLayer extends PlayerLayer {
	private static final int FULL_BRIGHT = 0xF000F0;

	public static IntFunction<Cosmetic> othersBack = id -> null;

	public CosmeticsLayer(RenderLayerParent<net.minecraft.client.player.AbstractClientPlayer, PlayerModel<net.minecraft.client.player.AbstractClientPlayer>> parent) {
		super(parent);
	}

	private ModelPart bone(Cosmetic.Anchor a) {
		PlayerModel m = getParentModel();
		switch (a) {
			case HEAD: return m.head;
			case BODY: return m.body;
			case RIGHT_ARM: return m.rightArm;
			default: return m.leftArm;
		}
	}

	@Override
	protected void draw(PoseStack pose, Parts out, int light, PlayerRenderState state) {
		Minecraft mc = Minecraft.getInstance();
		boolean mine = state.id == PlayerPreview.ID || mc.player != null && state.id == mc.player.getId();
		if (state.isInvisible) return;
		for (Slot slot : new Slot[] {Slot.HEAD, Slot.FACE, Slot.NECK, Slot.BACK, Slot.BODY}) {
			Cosmetic c = mine ? Wardrobe.equipped(slot) : slot == Slot.BACK ? othersBack.apply(state.id) : null;
			if (c == null) continue;
			if ((slot == Slot.HEAD || slot == Slot.FACE) && !state.headItem.isEmpty()) continue;
			if (slot == Slot.BACK && state.chestItem.getItem() == Items.ELYTRA) continue;
			for (Piece p : c.pieces()) {
				if (p.animator != null) p.animator.pose(p.part, state);
				pose.pushPose();
				bone(p.anchor).translateAndRotate(pose);
				int color = p.color != 0 ? p.color : mine ? Wardrobe.color(c) : c.defaultColor();
				out.part(p.part, p.texture(), p.glow ? FULL_BRIGHT : light, color);
				pose.popPose();
			}
		}
	}
}
