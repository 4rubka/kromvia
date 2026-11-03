package com.kromvia.client.compat;

import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.item.ItemStack;

public class PlayerRenderState extends EntityRenderState {
	public PlayerSkin skin;
	public float bodyRot;
	public float yRot;
	public float xRot;
	public boolean isFallFlying;
	public float capeFlap;
	public float capeLean;
	public ItemStack headItem = ItemStack.EMPTY;
	public ItemStack chestItem = ItemStack.EMPTY;
}
