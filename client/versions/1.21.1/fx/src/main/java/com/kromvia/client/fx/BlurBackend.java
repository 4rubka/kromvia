package com.kromvia.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class BlurBackend {
	private BlurBackend() {}

	static final boolean SUPPORTED = false;

	static Object load(Minecraft mc, String name) {
		return null;
	}

	static void draw(Minecraft mc, String name, Matrix4f inverse, Matrix4f previous, Vec3 delta, Object target, Object pool) {}
}
