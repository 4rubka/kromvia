package com.kromvia.client.cosmetic;

import com.kromvia.client.cosmetic.Cosmetic.Slot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import java.util.Random;

public final class Effects {
	private static final Random RANDOM = new Random();
	private static int tick;

	private Effects() {}

	public static void tick(Minecraft mc) {
		Cosmetic c = Wardrobe.equipped(Slot.EFFECT);
		if (c == null || mc.player == null || mc.level == null || mc.isPaused() || mc.player.isInvisible()) return;
		tick++;
		net.minecraft.client.player.LocalPlayer p = mc.player;
		double px = p.getX(), py = p.getY(), pz = p.getZ();
		if (c.id().equals("aura")) {
			double a = tick * 0.35;
			for (int i = 0; i < 2; i++) {
				double t = a + i * Math.PI;
				spawn(mc, ParticleTypes.END_ROD, px + Math.cos(t) * 0.8, py + 0.1, pz + Math.sin(t) * 0.8, 0, 0.01, 0);
			}
			return;
		}
		if (c.id().equals("rain_cloud")) {
			for (int i = 0; i < 2; i++) {
				spawn(mc, ParticleTypes.CLOUD, px + RANDOM.nextGaussian() * 0.3, py + 2.6, pz + RANDOM.nextGaussian() * 0.3, 0, 0, 0);
			}
			if (tick % 2 == 0) spawn(mc, ParticleTypes.DRIPPING_WATER, px + RANDOM.nextGaussian() * 0.3, py + 2.4, pz + RANDOM.nextGaussian() * 0.3, 0, 0, 0);
			return;
		}
		if (tick % 4 != 0) return;
		double angle = RANDOM.nextDouble() * Math.PI * 2;
		double r = 0.5 + RANDOM.nextDouble() * 0.3;
		double x = px + Math.cos(angle) * r;
		double y = py + 0.2 + RANDOM.nextDouble() * 1.6;
		double z = pz + Math.sin(angle) * r;
		switch (c.id()) {
			case "hearts": spawn(mc, ParticleTypes.HEART, x, y + 0.4, z, 0, 0.02, 0); break;
			case "flames": spawn(mc, ParticleTypes.FLAME, x, py + 0.1, z, 0, 0.02, 0); break;
			case "soul_flames": spawn(mc, ParticleTypes.SOUL_FIRE_FLAME, x, py + 0.1, z, 0, 0.02, 0); break;
			case "sparkles": spawn(mc, ParticleTypes.END_ROD, x, y, z, 0, 0.01, 0); break;
			case "notes": spawn(mc, ParticleTypes.NOTE, x, y + 0.5, z, RANDOM.nextDouble(), 0, 0); break;
			case "snow": spawn(mc, ParticleTypes.ITEM_SNOWBALL, x, py + 2.3, z, 0, -0.03, 0); break;
			case "petals": spawn(mc, ParticleTypes.CRIMSON_SPORE, x, py + 2.2, z, 0, 0, 0); break;
			case "totem": spawn(mc, ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 0, 0.1, 0); break;
			case "enchant": spawn(mc, ParticleTypes.ENCHANT, x, y + 0.6, z, px - x, -0.5, pz - z); break;
			case "glow": spawn(mc, ParticleTypes.EFFECT, x, y, z, 0, 0.01, 0); break;
			case "portal": spawn(mc, ParticleTypes.PORTAL, x, y, z, (px - x) * 2, -0.3, (pz - z) * 2); break;
			case "witch": spawn(mc, ParticleTypes.WITCH, x, y, z, 0, 0, 0); break;
			case "crit": spawn(mc, ParticleTypes.CRIT, x, y, z, 0, 0.1, 0); break;
			case "souls": spawn(mc, ParticleTypes.SOUL, x, py + 0.2, z, 0, 0.03, 0); break;
			case "ash": spawn(mc, ParticleTypes.WHITE_ASH, x, py + 2, z, 0, 0, 0); break;
			case "spores": spawn(mc, ParticleTypes.WARPED_SPORE, x, y, z, 0, 0, 0); break;
			case "fireworks": spawn(mc, ParticleTypes.FIREWORK, x, y, z, 0, 0.05, 0); break;
			case "sparks": spawn(mc, ParticleTypes.ENCHANTED_HIT, x, y, z, 0, 0, 0); break;
			case "rainbow": spawn(mc, rainbowDust(tick), x, py + 0.2, z, 0, 0, 0); break;
			default: break;
		}
	}

	private static ParticleOptions rainbowDust(int tick) {
		net.minecraft.world.phys.Vec3 c = net.minecraft.world.phys.Vec3.fromRGB24(Mth.hsvToRgb((tick % 120) / 120f, 0.8f, 1f));
		return new DustParticleOptions((float) c.x, (float) c.y, (float) c.z, 1.2f);
	}

	private static void spawn(Minecraft mc, ParticleOptions type, double x, double y, double z, double dx, double dy, double dz) {
		mc.level.addParticle(type, x, y, z, dx, dy, dz);
	}
}
