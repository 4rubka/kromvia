package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import java.util.List;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class FxChatComponentMixin {
	@Shadow
	@Final
	private List<GuiMessage> allMessages;

	@Shadow
	protected abstract void refreshTrimmedMessages();

	@ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private Component kromvia$compact(Component message) {
		message = Tweaks.hideName(message);
		if (!Tweaks.repeats(message)) return message;
		if (!allMessages.isEmpty()) {
			allMessages.removeFirst();
			refreshTrimmedMessages();
		}
		return Tweaks.counted(message);
	}

	@Inject(method = "clearMessages", at = @At("HEAD"), cancellable = true)
	private void kromvia$keep(boolean history, CallbackInfo ci) {
		if (history && Tweaks.keepChat()) ci.cancel();
	}
}
