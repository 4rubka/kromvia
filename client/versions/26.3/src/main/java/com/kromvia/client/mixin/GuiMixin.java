package com.kromvia.client.mixin;

import com.kromvia.client.ui.KromviaTitleScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public class GuiMixin {
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen kromvia$titleScreen(Screen screen) {
		if (screen == null && Minecraft.getInstance().level == null) return new KromviaTitleScreen();
		return screen instanceof TitleScreen ? new KromviaTitleScreen() : screen;
	}
}
