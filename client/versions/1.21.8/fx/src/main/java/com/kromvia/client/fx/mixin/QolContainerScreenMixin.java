package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class QolContainerScreenMixin extends Screen {
	@Shadow
	protected int leftPos;

	@Shadow
	protected int topPos;

	protected QolContainerScreenMixin(Component title) {
		super(title);
	}

	@Shadow
	protected abstract boolean hasClickedOutside(double x, double y, int left, int top, int button);

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void kromvia$clickOut(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
		var self = (AbstractContainerScreen<?>) (Object) this;
		if (Qol.clickOutCloses() && self.getMenu().getCarried().isEmpty() && hasClickedOutside(x, y, leftPos, topPos, button)) {
			onClose();
			cir.setReturnValue(true);
		}
	}
}
