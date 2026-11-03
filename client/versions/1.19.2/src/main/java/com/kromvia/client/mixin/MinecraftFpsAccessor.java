package com.kromvia.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftFpsAccessor {
	@Accessor("fps")
	static int kromvia$fps() {
		throw new AssertionError();
	}
}
