package com.kromvia.client.mixin;

import com.kromvia.client.ui.ModeCards;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$GameTab")
public class CreateWorldGameTabMixin {
	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;Lnet/minecraft/client/gui/layouts/LayoutSettings;)Lnet/minecraft/client/gui/layouts/LayoutElement;"), require = 0)
	private LayoutElement kromvia$modeCards(GridLayout.RowHelper helper, LayoutElement child, LayoutSettings settings, Operation<LayoutElement> original) {
		if (child instanceof CycleButton<?> b && b.getValue() instanceof WorldCreationUiState.SelectedGameMode) {
			original.call(helper, ModeCards.create(), settings);
			return child;
		}
		return original.call(helper, child, settings);
	}
}
