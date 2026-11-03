package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Qol;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class QolLivingEntityRenderStateMixin implements Qol.LivingState {
	@Unique
	private int kromvia$tint;

	@Unique
	private int kromvia$cracks = -1;

	@Unique
	private boolean kromvia$mount;

	@Override
	public boolean kromvia$mount() {
		return kromvia$mount;
	}

	@Override
	public void kromvia$mount(boolean faded) {
		kromvia$mount = faded;
	}

	@Override
	public int kromvia$tint() {
		return kromvia$tint;
	}

	@Override
	public int kromvia$cracks() {
		return kromvia$cracks;
	}

	@Override
	public void kromvia$set(int tint, int cracks) {
		kromvia$tint = tint;
		kromvia$cracks = cracks;
	}
}
