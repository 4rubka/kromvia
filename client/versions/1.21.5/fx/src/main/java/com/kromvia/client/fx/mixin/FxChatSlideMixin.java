package com.kromvia.client.fx.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;

/** Chat slide-in starts at 1.21.6 (Matrix3x2f GUI pose); not registered here. */
@Mixin(ChatComponent.class)
public class FxChatSlideMixin {}
