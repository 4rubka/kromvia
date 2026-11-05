package com.kromvia.client.module;

import com.kromvia.client.mixin.MinecraftUseInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class EasyPlace extends Module {
	static EasyPlace instance;
	private static int stage;
	private static boolean replaying, shiftWas;

	public EasyPlace() {
		super("easy_place", "Easy Place", Category.UTILITY, Corner.TOP_LEFT, false);
		icon(Items.OAK_TRAPDOOR);
		instance = this;
	}

	@Override
	public boolean hud() {
		return false;
	}

	private static boolean usable(Block b) {
		return b instanceof BaseEntityBlock || b instanceof TrapDoorBlock || b instanceof DoorBlock || b instanceof FenceGateBlock
				|| b instanceof ButtonBlock || b instanceof LeverBlock || b instanceof NoteBlock || b instanceof DiodeBlock
				|| b instanceof CraftingTableBlock || b instanceof AnvilBlock || b instanceof BedBlock || b instanceof CakeBlock
				|| b instanceof LoomBlock || b instanceof StonecutterBlock || b instanceof GrindstoneBlock
				|| b instanceof CartographyTableBlock || b instanceof FlowerPotBlock || b instanceof ComposterBlock;
	}

	public static boolean intercept() {
		if (instance == null || !instance.enabled || replaying) return false;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.player.isShiftKeyDown()) return false;
		if (!(mc.hitResult instanceof BlockHitResult) || mc.hitResult.getType() != HitResult.Type.BLOCK) return false;
		BlockHitResult hit = (BlockHitResult) mc.hitResult;
		if (!(mc.player.getMainHandItem().getItem() instanceof BlockItem) && !(mc.player.getOffhandItem().getItem() instanceof BlockItem)) return false;
		if (!usable(mc.level.getBlockState(hit.getBlockPos()).getBlock())) return false;
		if (stage == 0) {
			shiftWas = mc.options.keyShift.isDown();
			mc.options.keyShift.setDown(true);
			stage = 1;
		}
		return true;
	}

	@Override
	public void tick(Minecraft mc) {
		if (stage == 1) {
			stage = 2;
			replaying = true;
			try {
				((MinecraftUseInvoker) mc).kromvia$startUseItem();
			} finally {
				replaying = false;
			}
		} else if (stage == 2) {
			mc.options.keyShift.setDown(shiftWas);
			stage = 0;
		}
	}

	@Override
	public void disable(Minecraft mc) {
		if (stage != 0) mc.options.keyShift.setDown(shiftWas);
		stage = 0;
	}
}
