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
			case RIGHT_LEG: return m.rightLeg;
			case LEFT_LEG: return m.leftLeg;
			default: return m.leftArm;
		}
	}

	@Override
	protected void draw(PoseStack pose, Parts out, int light, PlayerRenderState state) {
		Minecraft mc = Minecraft.getInstance();
		boolean mine = state.id == PlayerPreview.ID || mc.player != null && state.id == mc.player.getId();
		if (state.isInvisible) return;
		boolean helmet = !state.headItem.isEmpty();
		boolean chest = !state.chestItem.isEmpty() && !(state.chestItem.getItem() == Items.ELYTRA);
		for (Slot slot : new Slot[] {Slot.HEAD, Slot.FACE, Slot.NECK, Slot.BACK, Slot.WEAPON, Slot.BODY}) {
			Cosmetic c = mine ? Wardrobe.equipped(slot) : slot == Slot.BACK ? othersBack.apply(state.id) : null;
			if (c == null) continue;
			if (slot == Slot.BACK && state.chestItem.getItem() == Items.ELYTRA) continue;
			for (Piece p : c.pieces()) {
				if (p.animator != null) p.animator.pose(p.part, state);
				pose.pushPose();
				bone(p.anchor).translateAndRotate(pose);
				if ((slot == Slot.BACK || slot == Slot.WEAPON) && chest && p.anchor == Cosmetic.Anchor.BODY) pose.translate(0, -0.053125F, 0.06875F);
				else if ((slot == Slot.HEAD || slot == Slot.FACE) && helmet && p.anchor == Cosmetic.Anchor.HEAD) overArmour(pose, 8, 8, 8, -4);
				else if ((slot == Slot.NECK || slot == Slot.BODY) && chest && p.anchor == Cosmetic.Anchor.BODY) overArmour(pose, 8, 12, 4, 6);
				int color = p.color != 0 ? p.color : mine ? Wardrobe.color(c) : c.defaultColor();
				out.part(p.part, p.texture(), p.glow ? FULL_BRIGHT : light, color);
				pose.popPose();
			}
		}
	}

	private static void overArmour(PoseStack pose, float w, float h, float d, float midY) {
		pose.translate(0, midY / 16, 0);
		pose.scale((w + 2) / w, (h + 2) / h, (d + 2) / d);
		pose.translate(0, -midY / 16, 0);
	}
}
