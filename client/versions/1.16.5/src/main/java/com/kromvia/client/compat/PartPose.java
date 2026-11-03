package com.kromvia.client.compat;

public final class PartPose {
	public static final PartPose ZERO = new PartPose(0, 0, 0, 0, 0, 0);

	public final float x, y, z, xRot, yRot, zRot;

	private PartPose(float x, float y, float z, float xRot, float yRot, float zRot) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.xRot = xRot;
		this.yRot = yRot;
		this.zRot = zRot;
	}

	public static PartPose offset(float x, float y, float z) {
		return new PartPose(x, y, z, 0, 0, 0);
	}

	public static PartPose offsetAndRotation(float x, float y, float z, float xRot, float yRot, float zRot) {
		return new PartPose(x, y, z, xRot, yRot, zRot);
	}
}
