package com.kromvia.client.cosmetic;

import java.util.Random;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.model.geom.ModelPart;
import com.kromvia.client.compat.PartPose;

final class Shapes {
	static final int SCALE = 4;

	static final class Built {
		private final ModelPart part;
		private final NativeImage image;
	
		Built(ModelPart part, NativeImage image) {
			this.part = part;
			this.image = image;
		}
	
		ModelPart part() {
			return part;
		}
	
		NativeImage image() {
			return image;
		}
	}

	private Shapes() {}

	private static int pow2(int n) {
		int p = 16;
		while (p < n) p <<= 1;
		return p;
	}

	static Built build(Pattern pattern, PartPose pose, float... b) {
		if (pattern == Pattern.FLAT) return flat(pose, b);
		int n = b.length / 6;
		int width = 64;
		for (int i = 0; i < n; i++) {
			width = Math.max(width, pow2(2 * ((int) Math.ceil(b[i * 6 + 3]) + (int) Math.ceil(b[i * 6 + 5]))));
		}
		int[] us = new int[n], vs = new int[n];
		int x = 0, y = 0, rowH = 0;
		for (int i = 0; i < n; i++) {
			int w = (int) Math.ceil(b[i * 6 + 3]), h = (int) Math.ceil(b[i * 6 + 4]), d = (int) Math.ceil(b[i * 6 + 5]);
			int uw = 2 * (w + d), vh = d + h;
			if (x + uw > width) {
				x = 0;
				y += rowH;
				rowH = 0;
			}
			us[i] = x;
			vs[i] = y;
			x += uw;
			rowH = Math.max(rowH, vh);
		}
		int height = pow2(y + rowH);

		ModelPart part = new ModelPart(width, height, 0, 0);
		for (int i = 0; i < n; i++) {
			part.texOffs(us[i], vs[i]).addBox(b[i * 6], b[i * 6 + 1], b[i * 6 + 2], b[i * 6 + 3], b[i * 6 + 4], b[i * 6 + 5]);
		}
		part.setPos(pose.x, pose.y, pose.z);
		part.xRot = pose.xRot;
		part.yRot = pose.yRot;
		part.zRot = pose.zRot;

		NativeImage img = new NativeImage(width * SCALE, height * SCALE, true);
		Random rnd = new Random(n * 31L + width);
		for (int i = 0; i < n; i++) {
			float w = b[i * 6 + 3], h = b[i * 6 + 4], d = b[i * 6 + 5];
			float u = us[i], v = vs[i];
			paint(img, pattern, Tex.Face.TOP, u + d, v, w, d, rnd);
			paint(img, pattern, Tex.Face.BOTTOM, u + d + w, v, w, d, rnd);
			paint(img, pattern, Tex.Face.WEST, u, v + d, d, h, rnd);
			paint(img, pattern, Tex.Face.NORTH, u + d, v + d, w, h, rnd);
			paint(img, pattern, Tex.Face.EAST, u + d + w, v + d, d, h, rnd);
			paint(img, pattern, Tex.Face.SOUTH, u + 2 * d + w, v + d, w, h, rnd);
		}
		return new Built(part, img);
	}

	private static Built flat(PartPose pose, float[] b) {
		int uw = 1, vh = 1;
		for (int i = 0; i < b.length; i += 6) {
			int w = (int) Math.ceil(b[i + 3]), h = (int) Math.ceil(b[i + 4]), d = (int) Math.ceil(b[i + 5]);
			uw = Math.max(uw, 2 * (w + d));
			vh = Math.max(vh, d + h);
		}
		int width = pow2(uw), height = pow2(vh);
		ModelPart part = new ModelPart(width, height, 0, 0);
		for (int i = 0; i < b.length; i += 6) part.texOffs(0, 0).addBox(b[i], b[i + 1], b[i + 2], b[i + 3], b[i + 4], b[i + 5]);
		part.setPos(pose.x, pose.y, pose.z);
		part.xRot = pose.xRot;
		part.yRot = pose.yRot;
		part.zRot = pose.zRot;
		NativeImage img = new NativeImage(width, height, true);
		img.fillRect(0, 0, width, height, 0xFFFFFFFF);
		return new Built(part, img);
	}

	static Built cape(NativeImage image) {
		ModelPart part = new ModelPart(64, 32, 0, 0);
		part.setPos(0, 0, 2.3f);
		ModelPart cloth = new ModelPart(64, 32, 0, 0);
		cloth.texOffs(0, 0).addBox(-5, 0, -1, 10, 16, 1);
		cloth.yRot = (float) Math.PI;
		part.addChild(cloth);
		return new Built(part, image);
	}

	private static void paint(NativeImage img, Pattern p, Tex.Face f, float u, float v, float w, float h, Random rnd) {
		int x0 = Math.round(u * SCALE), y0 = Math.round(v * SCALE);
		int x1 = Math.round((u + w) * SCALE), y1 = Math.round((v + h) * SCALE);
		Tex.face(img, p, f, x0, y0, x1 - x0, y1 - y0, rnd);
	}
}
