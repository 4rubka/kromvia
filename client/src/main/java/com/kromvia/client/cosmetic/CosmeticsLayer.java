package com.kromvia.client.cosmetic;

import com.kromvia.client.compat.PlayerLayer;
import com.kromvia.client.cosmetic.Cosmetic.Piece;
import com.kromvia.client.cosmetic.Cosmetic.Slot;
import com.kromvia.client.ui.PlayerPreview;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.IntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.item.Items;

public class CosmeticsLayer extends PlayerLayer {
	private static final int FULL_BRIGHT = 0xF000F0;

	public static IntFunction<Cosmetic> othersBack = id -> null;

	public CosmeticsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	private ModelPart bone(Cosmetic.Anchor a) {
		PlayerModel m = getParentModel();
		return switch (a) {
			case HEAD -> m.head;
			case BODY -> m.body;
			case RIGHT_ARM -> m.rightArm;
			case LEFT_ARM -> m.leftArm;
		};
	}

	@Override
	protected void draw(PoseStack pose, Parts out, int light, AvatarRenderState state) {
		Minecraft mc = Minecraft.getInstance();
		boolean mine = state.id == PlayerPreview.ID || mc.player != null && state.id == mc.player.getId();
		if (state.isInvisible) return;
		for (Slot slot : new Slot[] {Slot.HEAD, Slot.FACE, Slot.NECK, Slot.BACK, Slot.BODY, Slot.EFFECT}) {
			Cosmetic c = mine ? Wardrobe.equipped(slot) : slot == Slot.BACK ? othersBack.apply(state.id) : null;
			if (c == null) continue;
			if ((slot == Slot.HEAD || slot == Slot.FACE) && !state.headEquipment.isEmpty()) continue;
			if (slot == Slot.BACK && state.chestEquipment.is(Items.ELYTRA)) continue;
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
