package com.kromvia.client.fx.mixin;

import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Button.class)
public interface QolButtonAccessor {
	@Accessor("onPress")
	Button.OnPress kromvia$onPress();

	@Mutable
	@Accessor("onPress")
	void kromvia$setOnPress(Button.OnPress onPress);
}
