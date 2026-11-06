package com.kromvia.client.compat;

import net.minecraft.client.Minecraft;

public final class Reload {
	private Reload() {}

	/** True while the resource-reload overlay is up; screens may still render underneath (e.g. with RRLS). */
	public static boolean active() {
		return Minecraft.getInstance().getOverlay() != null;
	}
}
