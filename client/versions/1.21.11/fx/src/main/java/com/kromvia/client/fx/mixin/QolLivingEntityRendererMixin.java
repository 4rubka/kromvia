package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import com.kromvia.client.fx.Tweaks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class QolLivingEntityRendererMixin {
	static {
		Tweaks.OVERLAYS.fadesMount();
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
	private void kromvia$state(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
		((Qol.LivingState) state).kromvia$set(Qol.tintColumn(entity), Qol.cracks(entity));
		((Qol.LivingState) state).kromvia$mount(Tweaks.OVERLAYS.fadedMount(entity));
	}

	@WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
	private RenderType kromvia$mountType(LivingEntityRenderer<?, ?, ?> renderer, LivingEntityRenderState state, boolean bodyVisible, boolean translucent, boolean glowing, Operation<RenderType> original) {
		if (!((Qol.LivingState) state).kromvia$mount()) return original.call(renderer, state, bodyVisible, translucent, glowing);
		return Tweaks.OVERLAYS.hidesMount() ? null : original.call(renderer, state, bodyVisible, true, false);
	}

	@Inject(method = "shouldRenderLayers", at = @At("RETURN"), cancellable = true)
	private void kromvia$mountLayers(LivingEntityRenderState state, CallbackInfoReturnable<Boolean> cir) {
		if (((Qol.LivingState) state).kromvia$mount()) cir.setReturnValue(false);
	}

	@Inject(method = "getOverlayCoords", at = @At("RETURN"), cancellable = true)
	private static void kromvia$tint(LivingEntityRenderState state, float whiteOverlay, CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(Qol.overlay(cir.getReturnValueI(), ((Qol.LivingState) state).kromvia$tint()));
	}

	@WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
	private void kromvia$cracks(SubmitNodeCollector out, Model<Object> model, Object state, PoseStack pose, RenderType type, int light, int overlay, int color, TextureAtlasSprite sprite, int outline, ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original) {
		if (state instanceof Qol.LivingState m && m.kromvia$mount()) color = Tweaks.OVERLAYS.mountColor(color);
		int cracks = state instanceof Qol.LivingState s ? s.kromvia$cracks() : -1;
		if (crumbling == null && cracks >= 0) crumbling = new ModelFeatureRenderer.CrumblingOverlay(cracks, pose.last().copy());
		original.call(out, model, state, pose, type, light, overlay, color, sprite, outline, crumbling);
	}
}
