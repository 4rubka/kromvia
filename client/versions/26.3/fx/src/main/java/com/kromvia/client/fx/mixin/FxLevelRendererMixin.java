package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Fx;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class FxLevelRendererMixin {
	@Inject(method = "submitEntities", at = @At("TAIL"))
	private void kromvia$worldFx(PoseStack pose, LevelRenderState state, SubmitNodeCollector out, CallbackInfo ci) {
		Fx.submitWorld(pose, out, state.cameraRenderState.pos);
	}
}
