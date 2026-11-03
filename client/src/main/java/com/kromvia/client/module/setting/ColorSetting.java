package com.kromvia.client.module.setting;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class ColorSetting extends Setting<Integer> {
	public static final List<Integer> PALETTE = List.of(
			0xFFFFFFFF, 0xFFAAAAAA, 0xFF262626, 0xFFFF4040, 0xFFF08A24, 0xFFFFC83D, 0xFFFFF15A,
			0xFF8B5CF6, 0xFF2E8B3E, 0xFF4FE3E8, 0xFF3A6FD9, 0xFF8E44D9, 0xFFFF5FA2);

	public ColorSetting(String id, String name, int value) {
		super(id, name, value);
	}

	public int argb() {
		return value;
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(String.format("#%08X", value));
	}

	@Override
	public void load(JsonElement json) {
		if (!json.isJsonPrimitive()) return;
		try {
			value = (int) Long.parseLong(json.getAsString().replace("#", ""), 16);
		} catch (NumberFormatException ignored) {
		}
	}
}
