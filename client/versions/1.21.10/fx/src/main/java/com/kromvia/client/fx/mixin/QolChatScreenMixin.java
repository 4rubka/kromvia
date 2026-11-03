package com.kromvia.client.fx.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.fx.Qol;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class QolChatScreenMixin extends Screen {
	protected QolChatScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void kromvia$copyOrDelete(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && Qol.chatClick(minecraft, (Qol.ChatLines) minecraft.gui.getChat(), event.x(), event.y())) {
			cir.setReturnValue(true);
		}
	}

	@Redirect(method = "mouseClicked", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/ChatScreen;handleComponentClicked(Lnet/minecraft/network/chat/Style;)Z"))
	private boolean kromvia$ourClicks(ChatScreen screen, Style style) {
		return Qol.clicked(minecraft, style) || screen.handleComponentClicked(style);
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void kromvia$preview(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!Qol.TOOLS.enabled) return;
		String url = Qol.imageUrl(minecraft.gui.getChat().getClickedComponentStyleAt(mouseX, mouseY));
		if (url != null) Qol.preview(Gfx.of(g), minecraft, url, mouseX, mouseY);
	}
}
