package com.kromvia.client;

public final class Launcher {
	private Launcher() {}

	public static final boolean PRESENT = System.getenv("KROMVIA_PROFILES") != null;
}
