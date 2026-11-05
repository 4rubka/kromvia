package com.kromvia.client.fx.mixin;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;

/** Scoreboard tweaks start at 1.21.6 (Matrix3x2f GUI pose); not registered here. */
@Mixin(Gui.class)
public class FxScoreboardMixin {}
