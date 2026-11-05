package com.kromvia.client.module;

import net.minecraft.core.particles.ParticleOptions;
import com.kromvia.client.module.setting.ChoiceSetting;
import com.kromvia.client.compat.Mc;
import com.kromvia.client.compat.Gfx;
import com.kromvia.client.KromviaClient;
import com.kromvia.client.mixin.OverlayTextureAccessor;
import com.kromvia.client.module.setting.BoolSetting;
import com.kromvia.client.module.setting.ColorSetting;
import com.kromvia.client.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

public final class Visuals {
	private Visuals() {}

	private static class Flag extends Module {
		Flag(String id, String name, Category cat, boolean on) {
			super(id, name, cat, Corner.TOP_LEFT, on);
		}

		@Override
		public boolean hud() {
			return false;
		}
	}

	public static final Module NO_FOG = new Flag("no_fog", "No Fog", Module.Category.UTILITY, false).icon(Items.GLASS);

	public static final class BlockOutline extends Flag {
		public final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF8B5CF6));
		final SliderSetting opacity = add(new SliderSetting("opacity", "Opacity", 80, 10, 100, 5, "%"));
		final SliderSetting thickness = add(new SliderSetting("thickness", "Thickness", 1.5F, 1, 4, 0.5F, "x"));

		BlockOutline() {
			super("block_outline", "Block Outline", Module.Category.UTILITY, false);
			icon(Items.GRASS_BLOCK);
		}

		public int argb() {
			return ARGB.color(Math.round(opacity.value() * 2.55F), color.argb());
		}

		public float width(float vanilla) {
			return vanilla * thickness.value();
		}
	}

	public static final BlockOutline OUTLINE = new BlockOutline();

	public static final class HitColor extends Flag {
		private static final int VANILLA = 0xB2FF0000;
		final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF3A6FD9));
		final SliderSetting opacity = add(new SliderSetting("opacity", "Opacity", 70, 10, 100, 5, "%"));
		private int painted = VANILLA;

		HitColor() {
			super("hit_color", "Hit Colour", Module.Category.PVP, false);
			icon(Mc.redDye());
		}

		private void paint(Minecraft mc, int argb) {
			if (argb == painted) return;
			var tex = ((OverlayTextureAccessor) mc.gameRenderer.overlayTexture()).kromvia$texture();
			var img = tex.getPixels();
			for (int y = 0; y < 8; y++) {
				for (int x = 0; x < 16; x++) img.setPixel(x, y, argb);
			}
			tex.upload();
			painted = argb;
		}

		@Override
		public void tick(Minecraft mc) {
			paint(mc, ARGB.color(Math.round(opacity.value() * 2.55F), color.argb()));
		}

		@Override
		public void disable(Minecraft mc) {
			paint(mc, VANILLA);
		}
	}

	public static final HitColor HIT_COLOR = new HitColor();

	private static LivingEntity target;
	private static long targetAt;

	public static final class HitParticles extends Flag {
		final SliderSetting amount = add(new SliderSetting("amount", "Extra bursts", 2, 1, 5, 1, "x"));
		final ChoiceSetting type = add(new ChoiceSetting("type", "Particle", 0, "Crit", "Magic", "Flame", "Hearts", "Totem", "End Rod", "Notes"));
		final BoolSetting sharpness = add(new BoolSetting("sharpness", "Sharpness sparks", true));

		HitParticles() {
			super("hit_particles", "Hit Particles", Module.Category.PVP, false);
			icon(Items.NETHER_STAR);
		}
	}

	public static final HitParticles PARTICLES = new HitParticles();

	private static ParticleOptions hitParticle() {
		switch (PARTICLES.type.index()) {
			case 1: return ParticleTypes.ENCHANTED_HIT;
			case 2: return ParticleTypes.FLAME;
			case 3: return ParticleTypes.HEART;
			case 4: return ParticleTypes.TOTEM_OF_UNDYING;
			case 5: return ParticleTypes.END_ROD;
			case 6: return ParticleTypes.NOTE;
			default: return ParticleTypes.CRIT;
		}
	}

	public static void onAttack(Minecraft mc, Entity hit) {
		if (hit instanceof LivingEntity le) {
			target = le;
			targetAt = System.currentTimeMillis();
		}
		if (!PARTICLES.enabled) return;
		for (int i = 0; i < Math.round(PARTICLES.amount.value()); i++) {
			mc.particleEngine.createTrackingEmitter(hit, hitParticle());
			if (PARTICLES.sharpness.on()) mc.particleEngine.createTrackingEmitter(hit, ParticleTypes.ENCHANTED_HIT);
		}
	}

	public static final Module TARGET_HUD = new Module("target_hud", "Target HUD", Module.Category.PVP, Module.Corner.TOP_LEFT, false) {
		private static final int W = 130, H = 32;

		private LivingEntity current(Minecraft mc) {
			if (mc.player == null) return null;
			if (mc.crosshairPickEntity instanceof LivingEntity le && le.isAlive()) return le;
			boolean fresh = System.currentTimeMillis() - targetAt < 4000;
			return fresh && target != null && target.isAlive() && target.level() == mc.level ? target : null;
		}

		@Override
		public boolean visible(Minecraft mc) {
			return current(mc) != null;
		}

		@Override
		public int width(Minecraft mc) {
			return W;
		}

		@Override
		public int height(Minecraft mc) {
			return H;
		}

		@Override
		public void render(Gfx g, Minecraft mc, int x, int y) {
			LivingEntity e = current(mc);
			String name = e == null ? "Steve" : e.getDisplayName().getString();
			float hp = e == null ? 14 : e.getHealth(), max = e == null ? 20 : e.getMaxHealth();
			float abs = e == null ? 0 : e.getAbsorptionAmount();
			float dist = e == null ? 3.1F : mc.player.distanceTo(e);

			g.fill(x, y, x + W, y + H, TextModule.PLATE);
			g.fill(x, y, x + 2, y + H, 0xFF8B5CF6);
			g.drawString(mc.font, mc.font.plainSubstrByWidth(name, W - 50), x + 6, y + 4, 0xFFFFFFFF, true);
			String d = String.format("%.1fm", dist);
			g.drawString(mc.font, d, x + W - 4 - mc.font.width(d), y + 4, 0xFFAAAAAA, true);

			float f = Mth.clamp(hp / max, 0, 1);
			int bar = W - 12, bx = x + 6, by = y + 17;
			int col = ARGB.srgbLerp(f, 0xFFFF4040, 0xFF8B5CF6);
			g.fill(bx, by, bx + bar, by + 10, 0xFF262626);
			g.fill(bx, by, bx + Math.round(bar * f), by + 10, col);
			if (abs > 0) g.fill(bx, by, bx + Math.round(bar * Mth.clamp(abs / max, 0, 1)), by + 2, 0xFFFFC83D);
			String h = abs > 0 ? String.format("%.1f +%.0f", hp, abs) : String.format("%.1f", hp);
			g.drawString(mc.font, h, bx + (bar - mc.font.width(h)) / 2, by + 1, 0xFFFFFFFF, true);
		}
	}.icon(Items.GOLDEN_SWORD);
}
