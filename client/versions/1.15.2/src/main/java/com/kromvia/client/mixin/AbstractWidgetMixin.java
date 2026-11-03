package com.kromvia.client.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.ui.UiStyle;
import net.minecraft.client.Minecraft;
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
	protected int width;

	@Shadow
	protected int height;

	@Shadow
	protected float alpha;

	@Shadow
	protected boolean isHovered;

	@Inject(method = "renderButton", at = @At("HEAD"), cancellable = true)
	private void kromvia$style(int mouseX, int mouseY, float delta, CallbackInfo ci) {
		AbstractWidget self = (AbstractWidget) (Object) this;
		if (!(self instanceof AbstractButton)) return;
		Gfx g = Gfx.of();
		UiStyle.button(g, self.x, self.y, width, height, self.active, isHovered);
		g.drawCenteredString(Minecraft.getInstance().font, self.getMessage(), self.x + width / 2,
				self.y + (height - 8) / 2, (self.active ? 0xFFFFFF : 0xA0A0A0) | Mth.ceil(alpha * 255) << 24);
		ci.cancel();
	}
}
