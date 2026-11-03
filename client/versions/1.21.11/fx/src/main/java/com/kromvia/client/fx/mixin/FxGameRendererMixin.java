package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Fx;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class FxGameRendererMixin {
	@Shadow
	@Final
	private CrossFrameResourcePool resourcePool;

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = At.Shift.AFTER))
	private void kromvia$motionBlur(CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		Fx.MOTION_BLUR.render(mc, mc.getMainRenderTarget(), resourcePool);
		Fx.SATURATION.render(mc, mc.getMainRenderTarget(), resourcePool);
	}
}
