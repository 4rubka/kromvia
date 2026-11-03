package com.kromvia.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

@FunctionalInterface
public interface Geometry {
	void draw(PoseStack.Pose pose, VertexConsumer buffer);
}
