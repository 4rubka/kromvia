package com.kromvia.client.module;

import java.util.ArrayDeque;

public final class Clicks {
	private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
	private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();

	private Clicks() {}

	public static void press(int button) {
		if (button == 0) LEFT.addLast(System.currentTimeMillis());
		else if (button == 1) RIGHT.addLast(System.currentTimeMillis());
	}

	public static int left() {
		return count(LEFT);
	}

	public static int right() {
		return count(RIGHT);
	}

	private static int count(ArrayDeque<Long> clicks) {
		long cutoff = System.currentTimeMillis() - 1000;
		while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) clicks.removeFirst();
		return clicks.size();
	}
}
