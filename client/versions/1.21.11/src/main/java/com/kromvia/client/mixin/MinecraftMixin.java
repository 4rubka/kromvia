package com.kromvia.client.mixin;

import com.kromvia.client.Rooms;
import com.kromvia.client.cosmetic.Effects;
import com.kromvia.client.module.Hud;
import com.kromvia.client.ui.KromviaTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void kromvia$tickEffects(CallbackInfo ci) {
		Effects.tick((Minecraft) (Object) this);
		Hud.tick((Minecraft) (Object) this);
		Rooms.tick((Minecraft) (Object) this);
	}

	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen kromvia$titleScreen(Screen screen) {
		if (screen == null && ((Minecraft) (Object) this).level == null) return new KromviaTitleScreen();
		return screen instanceof TitleScreen ? new KromviaTitleScreen() : screen;
	}
}
