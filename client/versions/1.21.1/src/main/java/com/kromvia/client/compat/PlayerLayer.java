package com.kromvia.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;

public abstract class PlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	protected PlayerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
		super(parent);
	}

	@Override
	public final void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float delta, float ageInTicks, float yRot, float xRot) {
		PlayerRenderState state = new PlayerRenderState();
		state.id = player.getId();
		state.ageInTicks = ageInTicks;
		state.isInvisible = player.isInvisible();
		state.isFallFlying = player.isFallFlying();
		state.headItem = player.getItemBySlot(EquipmentSlot.HEAD);
		state.chestItem = player.getItemBySlot(EquipmentSlot.CHEST);
		double dx = Mth.lerp(delta, player.xCloakO, player.xCloak) - Mth.lerp(delta, player.xo, player.getX());
		double dy = Mth.lerp(delta, player.yCloakO, player.yCloak) - Mth.lerp(delta, player.yo, player.getY());
		double dz = Mth.lerp(delta, player.zCloakO, player.zCloak) - Mth.lerp(delta, player.zo, player.getZ());
		float body = Mth.rotLerp(delta, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
		state.capeLean = Mth.clamp((float) (dx * Mth.sin(body) - dz * Mth.cos(body)) * 100, 0, 150);
		state.capeFlap = Mth.clamp((float) dy * 10, -6, 32)
				+ Mth.sin(Mth.lerp(delta, player.walkDistO, player.walkDist) * 6) * 32 * Mth.lerp(delta, player.oBob, player.bob);
		render(pose, buffers, light, state);
	}

	public final void render(PoseStack pose, MultiBufferSource buffers, int light, PlayerRenderState state) {
		draw(pose, (part, texture, partLight, color) ->
				part.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), partLight, OverlayTexture.NO_OVERLAY, color), light, state);
	}

	protected abstract void draw(PoseStack pose, Parts out, int light, PlayerRenderState state);

	public interface Parts {
		void part(ModelPart part, ResourceLocation texture, int light, int color);
	}
}
