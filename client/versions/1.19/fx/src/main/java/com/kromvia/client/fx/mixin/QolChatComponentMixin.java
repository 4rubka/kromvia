package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import java.util.List;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
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
	private List<GuiMessage<Component>> allMessages;

	@Shadow
	@Final
	private List<GuiMessage<FormattedCharSequence>> trimmedMessages;

	@Shadow
	private int chatScrollbarPos;

	@Shadow
	protected abstract void refreshTrimmedMessage();

	@Shadow
	protected abstract double getScale();

	@Shadow
	protected abstract int getWidth();

	@Shadow
	public abstract int getLinesPerPage();

	@Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;IIZ)V",
			at = @At("HEAD"), cancellable = true)
	private void kromvia$filter(Component message, int id, int time, boolean refresh, CallbackInfo ci) {
		if (Qol.filter(message) == null) ci.cancel();
	}

	@ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;IIZ)V",
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
		int line = row + chatScrollbarPos, index = 0;
		for (int i = 1; i <= line; i++) {
			GuiMessage<FormattedCharSequence> a = trimmedMessages.get(i - 1), b = trimmedMessages.get(i);
			if (a.getAddedTime() != b.getAddedTime() || a.getId() != b.getId()) index++;
		}
		if (index >= allMessages.size()) return null;
		GuiMessage<Component> message = allMessages.get(index);
		if (delete) {
			allMessages.remove(index);
			refreshTrimmedMessage();
		}
		return message.getMessage();
	}
}
