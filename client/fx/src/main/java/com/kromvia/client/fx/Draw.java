package com.kromvia.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;

final class Draw {
	private Draw() {}

	static int alpha(int argb, float a) {
		return Math.round(Mth.clamp(a, 0, 1) * (argb >>> 24)) << 24 | argb & 0xFFFFFF;
	}

	static void vertex(VertexConsumer b, PoseStack.Pose p, double x, double y, double z, int argb) {
		Types.vertex(b, p, x, y, z, argb);
	}

	static void ring(VertexConsumer b, PoseStack.Pose p, double x, double y, double z, float r0, float r1, int c0, int c1, int segments) {
		for (int i = 0; i < segments; i++) {
			float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
			float s0 = Mth.sin(a0), k0 = Mth.cos(a0), s1 = Mth.sin(a1), k1 = Mth.cos(a1);
			vertex(b, p, x + k0 * r0, y, z + s0 * r0, c0);
			vertex(b, p, x + k0 * r1, y, z + s0 * r1, c1);
			vertex(b, p, x + k1 * r1, y, z + s1 * r1, c1);
			vertex(b, p, x + k1 * r0, y, z + s1 * r0, c0);
		}
	}

	static void band(VertexConsumer b, PoseStack.Pose p, double x, double y, double z, float r, float h, int c0, int c1, int segments) {
		for (int i = 0; i < segments; i++) {
			float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
			double x0 = x + Mth.cos(a0) * r, z0 = z + Mth.sin(a0) * r, x1 = x + Mth.cos(a1) * r, z1 = z + Mth.sin(a1) * r;
			vertex(b, p, x0, y, z0, c0);
			vertex(b, p, x0, y + h, z0, c1);
			vertex(b, p, x1, y + h, z1, c1);
			vertex(b, p, x1, y, z1, c0);
		}
	}

	static void cone(VertexConsumer b, PoseStack.Pose p, double x, double y, double z, float r, float h, int[] colors, float baseAlpha, float tipAlpha) {
		int n = colors.length;
		for (int i = 0; i < n; i++) {
			float a0 = Mth.TWO_PI * i / n, a1 = Mth.TWO_PI * (i + 1) / n;
			int c = alpha(colors[i], baseAlpha), tip = alpha(colors[i], tipAlpha);
			vertex(b, p, x + Mth.cos(a0) * r, y, z + Mth.sin(a0) * r, c);
			vertex(b, p, x + Mth.cos(a1) * r, y, z + Mth.sin(a1) * r, c);
			vertex(b, p, x, y + h, z, tip);
			vertex(b, p, x, y + h, z, tip);
		}
	}

	static void facingRing(VertexConsumer b, PoseStack.Pose p, double x, double y, double z, float r0, float r1, int c0, int c1, int segments) {
		double len = Math.sqrt(x * x + y * y + z * z);
		if (len < 1e-4) return;
		double dx = x / len, dy = y / len, dz = z / len;
		double rx = -dz, rz = dx, rl = Math.sqrt(rx * rx + rz * rz);
		if (rl < 1e-4) {
			rx = 1;
			rz = 0;
			rl = 1;
		}
		rx /= rl;
		rz /= rl;
		double ux = -rz * dy, uy = rz * dx - rx * dz, uz = rx * dy;
		for (int i = 0; i < segments; i++) {
			float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
			float k0 = Mth.cos(a0), s0 = Mth.sin(a0), k1 = Mth.cos(a1), s1 = Mth.sin(a1);
			vertex(b, p, x + (rx * k0 + ux * s0) * r0, y + uy * s0 * r0, z + (rz * k0 + uz * s0) * r0, c0);
			vertex(b, p, x + (rx * k0 + ux * s0) * r1, y + uy * s0 * r1, z + (rz * k0 + uz * s0) * r1, c1);
			vertex(b, p, x + (rx * k1 + ux * s1) * r1, y + uy * s1 * r1, z + (rz * k1 + uz * s1) * r1, c1);
			vertex(b, p, x + (rx * k1 + ux * s1) * r0, y + uy * s1 * r0, z + (rz * k1 + uz * s1) * r0, c0);
		}
	}

	static void line(VertexConsumer b, PoseStack.Pose p, double x0, double y0, double z0, double x1, double y1, double z1, double w, int c) {
		double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
		double vx = (x0 + x1) / 2, vy = (y0 + y1) / 2, vz = (z0 + z1) / 2;
		double nx = dy * vz - dz * vy, ny = dz * vx - dx * vz, nz = dx * vy - dy * vx;
		double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (nl < 1e-6) return;
		double k = w / 2 / nl;
		nx *= k;
		ny *= k;
		nz *= k;
		vertex(b, p, x0 + nx, y0 + ny, z0 + nz, c);
		vertex(b, p, x0 - nx, y0 - ny, z0 - nz, c);
		vertex(b, p, x1 - nx, y1 - ny, z1 - nz, c);
		vertex(b, p, x1 + nx, y1 + ny, z1 + nz, c);
	}
}
