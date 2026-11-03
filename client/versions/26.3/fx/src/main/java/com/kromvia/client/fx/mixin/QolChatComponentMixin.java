package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class QolChatComponentMixin implements Qol.ChatLines {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	@Final
	private List<GuiMessage> allMessages;

	@Shadow
	@Final
	private List<GuiMessage.Line> trimmedMessages;

	@Shadow
	private int chatScrollbarPos;

	@Shadow
	protected abstract void refreshTrimmedMessages();

	@Shadow
	protected abstract double getScale();

	@Shadow
	protected abstract int getWidth();

	@Shadow
	public abstract int getLinesPerPage();

	@Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
			at = @At("HEAD"), cancellable = true)
	private void kromvia$filter(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
		if (Qol.filter(message) == null) ci.cancel();
	}

	@ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private Component kromvia$mask(Component message) {
		Component shown = Qol.filter(message);
		return Qol.screenshotButtons(shown == null ? message : shown);
	}

	@Override
	public @Nullable Component kromvia$at(double mouseX, double mouseY, boolean delete) {
		double scale = getScale();
		int bottom = Mth.floor((minecraft.getWindow().getGuiScaledHeight() - 40) / scale);
		int entry = (int) (9 * (minecraft.options.chatLineSpacing().get() + 1.0));
		double x = mouseX / scale - 4;
		if (x < -4 || x > Mth.ceil(getWidth() / scale) + 8) return null;
		int row = Mth.floor((bottom - mouseY / scale) / entry);
		if (row < 0 || row >= Math.min(getLinesPerPage(), trimmedMessages.size() - chatScrollbarPos)) return null;
		GuiMessage message = trimmedMessages.get(row + chatScrollbarPos).parent();
		if (delete) {
			allMessages.remove(message);
			refreshTrimmedMessages();
		}
		return message.content();
	}
}
