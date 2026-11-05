package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.components.ChatComponent;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChatComponent.class)
public class FxChatSlideMixin {
	static {
		Tweaks.CHAT.slide.shown = true;
	}

	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;pushMatrix()Lorg/joml/Matrix3x2fStack;", ordinal = 0))
	private Matrix3x2fStack kromvia$slide(Matrix3x2fStack pose, Operation<Matrix3x2fStack> op) {
		Matrix3x2fStack out = op.call(pose);
		float dy = Tweaks.chatSlide();
		if (dy != 0) pose.translate(0, dy);
		return out;
	}
}
