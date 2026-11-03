package com.kromvia.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class TextSetting extends Setting<String> {
	public final int max;

	public TextSetting(String id, String name, String value, int max) {
		super(id, name, value);
		this.max = max;
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement json) {
		if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) value = json.getAsString();
	}
}
