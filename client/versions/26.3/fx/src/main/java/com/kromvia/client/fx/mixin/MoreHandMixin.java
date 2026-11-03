package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.More;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class MoreHandMixin {
	@ModifyConstant(method = "submitHandsWithItems", constant = @Constant(floatValue = 0.1F))
	private float kromvia$sway(float c) {
		return More.sway(c);
	}
}
