package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Tweaks;
import java.util.List;
import net.minecraft.client.GuiMessage;
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
	private List<GuiMessage<Component>> allMessages;

	@Shadow
	protected abstract void refreshTrimmedMessage();

	@ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;IIZ)V",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private Component kromvia$compact(Component message) {
		message = Tweaks.hideName(message);
		if (!Tweaks.repeats(message)) return message;
		if (!allMessages.isEmpty()) {
			allMessages.remove(0);
			refreshTrimmedMessage();
		}
		return Tweaks.counted(message);
	}

	@Inject(method = "clearMessages", at = @At("HEAD"), cancellable = true)
	private void kromvia$keep(boolean history, CallbackInfo ci) {
		if (history && Tweaks.keepChat()) ci.cancel();
	}
}
