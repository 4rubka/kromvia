package com.kromvia.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public abstract class PlayerLayer extends RenderLayer<PlayerRenderState, PlayerModel> {
	protected PlayerLayer(RenderLayerParent<PlayerRenderState, PlayerModel> parent) {
		super(parent);
	}

	@Override
	public final void render(PoseStack pose, MultiBufferSource buffers, int light, PlayerRenderState state, float yRot, float xRot) {
		draw(pose, (part, texture, partLight, color) ->
				part.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), partLight, OverlayTexture.NO_OVERLAY, color), light, state);
	}

	protected abstract void draw(PoseStack pose, Parts out, int light, PlayerRenderState state);

	public interface Parts {
		void part(ModelPart part, ResourceLocation texture, int light, int color);
	}
}
