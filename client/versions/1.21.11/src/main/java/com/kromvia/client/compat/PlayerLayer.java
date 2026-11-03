package com.kromvia.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public abstract class PlayerLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	protected PlayerLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	@Override
	public final void submit(PoseStack pose, SubmitNodeCollector out, int light, AvatarRenderState state, float yRot, float xRot) {
		draw(pose, (part, texture, partLight, color) ->
				out.submitModelPart(part, pose, RenderTypes.entityCutoutNoCull(texture), partLight, OverlayTexture.NO_OVERLAY, null, false, false, color, null, 0), light, state);
	}

	protected abstract void draw(PoseStack pose, Parts out, int light, AvatarRenderState state);

	public interface Parts {
		void part(ModelPart part, Identifier texture, int light, int color);
	}
}
