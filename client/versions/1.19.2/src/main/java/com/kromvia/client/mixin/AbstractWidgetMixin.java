package com.kromvia.client.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.ui.UiStyle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
	@Shadow
	protected float alpha;

	@Inject(method = "renderButton", at = @At("HEAD"), cancellable = true)
	private void kromvia$style(PoseStack g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		AbstractWidget self = (AbstractWidget) (Object) this;
		if (!(self instanceof AbstractButton)) return;
		UiStyle.button(Gfx.of(g), self.x, self.y, self.getWidth(), self.getHeight(), self.active, self.isHoveredOrFocused());
		GuiComponent.drawCenteredString(g, Minecraft.getInstance().font, self.getMessage(), self.x + self.getWidth() / 2,
				self.y + (self.getHeight() - 8) / 2, (self.active ? 0xFFFFFF : 0xA0A0A0) | Mth.ceil(alpha * 255) << 24);
		ci.cancel();
	}
}
