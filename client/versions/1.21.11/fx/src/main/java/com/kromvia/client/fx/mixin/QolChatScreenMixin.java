package com.kromvia.client.fx.mixin;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.fx.Qol;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

	@Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
	private void kromvia$ourClicks(Style style, boolean insertions, CallbackInfoReturnable<Boolean> cir) {
		if (Qol.clicked(minecraft, style)) cir.setReturnValue(true);
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void kromvia$preview(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!Qol.TOOLS.enabled) return;
		ActiveTextCollector.ClickableStyleFinder finder = new ActiveTextCollector.ClickableStyleFinder(font, mouseX, mouseY);
		minecraft.gui.getChat().captureClickableText(finder, minecraft.getWindow().getGuiScaledHeight(), minecraft.gui.getGuiTicks(), true);
		String url = Qol.imageUrl(finder.result());
		if (url != null) Qol.preview(Gfx.of(g), minecraft, url, mouseX, mouseY);
	}
}
