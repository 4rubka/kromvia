package com.kromvia.client.cosmetic;

import net.minecraft.client.model.geom.ModelPart;

final class PartScale {
	private PartScale() {}

	static void set(ModelPart part, float scale) {
		part.xScale = part.yScale = part.zScale = scale;
		part.visible = scale > 0.02f;
	}
}
