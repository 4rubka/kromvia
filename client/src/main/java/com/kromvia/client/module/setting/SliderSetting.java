package com.kromvia.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.Mth;

public final class SliderSetting extends Setting<Float> {
	public final float min;
	public final float max;
	public final float step;
	public final String unit;

	public SliderSetting(String id, String name, float value, float min, float max, float step, String unit) {
		super(id, name, value);
		this.min = min;
		this.max = max;
		this.step = step;
		this.unit = unit;
	}

	public float value() {
		return value;
	}

	public double fraction() {
		return (value - min) / (max - min);
	}

	public void setFraction(double f) {
		float v = min + (float) Mth.clamp(f, 0, 1) * (max - min);
		value = Math.round(v / step) * step;
	}

	public String label() {
		boolean whole = step >= 1;
		return name + ": " + (whole ? String.valueOf(Math.round(value)) : String.format("%.1f", value)) + unit;
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement json) {
		if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) value = Mth.clamp(json.getAsFloat(), min, max);
	}
}
