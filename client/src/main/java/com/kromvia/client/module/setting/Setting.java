package com.kromvia.client.module.setting;

import com.google.gson.JsonElement;

public abstract sealed class Setting<T> permits BoolSetting, ChoiceSetting, ColorSetting, SliderSetting, TextSetting {
	public final String id;
	public final String name;
	protected T value;
	public boolean shown = true;

	protected Setting(String id, String name, T value) {
		this.id = id;
		this.name = name;
		this.value = value;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = value;
	}

	public abstract JsonElement save();

	public abstract void load(JsonElement json);
}
