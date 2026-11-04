package com.kromvia.client.cosmetic;

import net.minecraft.client.model.geom.ModelPart;

/** ModelPart has no scale before 1.19, so a shrinking sprite just disappears halfway. */
final class PartScale {
	private PartScale() {}

	static void set(ModelPart part, float scale) {
		part.visible = scale > 0.5f;
	}
}
