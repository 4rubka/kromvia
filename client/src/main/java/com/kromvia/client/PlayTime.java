package com.kromvia.client;

public final class PlayTime {
	private PlayTime() {}

	private static volatile long build = -1;
	private static volatile long total;

	public static void set(long buildSeconds, long totalSeconds) {
		build = buildSeconds;
		total = totalSeconds;
	}

	public static String line() {
		if (build < 0) return null;
		return hours(build) + " on this build · " + hours(total) + " total";
	}

	private static String hours(long seconds) {
		long h = seconds / 3600;
		return h > 0 ? h + "h" : seconds / 60 + "m";
	}
}
