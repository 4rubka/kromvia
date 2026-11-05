package com.kromvia.client.module;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.module.setting.BoolSetting;
import com.kromvia.client.module.setting.ChoiceSetting;
import com.kromvia.client.module.setting.ColorSetting;
import com.kromvia.client.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public abstract class TextModule extends Module {
	static final int PLATE = 0x90000000;

	public final ChoiceSetting colorMode = add(new ChoiceSetting("color_mode", "Colour", 0, "Solid", "Gradient", "Rainbow", "Flowing gradient", "Pulse", "Rainbow (one colour)"));
	public final ColorSetting color = add(new ColorSetting("color", "Main colour", 0xFFFFFFFF));
	public final ColorSetting color2 = add(new ColorSetting("color2", "Gradient end", 0xFF8B5CF6));
	public final SliderSetting animSpeed = add(new SliderSetting("speed", "Animation speed", 100, 10, 300, 10, "%"));
	public final SliderSetting rainbowSaturation = add(new SliderSetting("saturation", "Rainbow saturation", 65, 0, 100, 5, "%"));
	public final BoolSetting background = add(new BoolSetting("background", "Background", true));
	public final ColorSetting backgroundColor = add(new ColorSetting("background_color", "Background colour", 0xFF000000));
	public final ChoiceSetting accentLine = add(new ChoiceSetting("accent", "Accent line", 0, "None", "Left", "Bottom", "Outline"));
	public final SliderSetting opacity = add(new SliderSetting("opacity", "Background opacity", 55, 0, 100, 5, "%"));
	public final BoolSetting shadow = add(new BoolSetting("shadow", "Text shadow", true));
	public final ChoiceSetting brackets = add(new ChoiceSetting("brackets", "Brackets", 0, "None", "[ ]", "( )", "< >"));
	public final SliderSetting size = add(new SliderSetting("scale", "Size", 100, 50, 200, 10, "%"));

	protected TextModule(String id, String name, Category category, boolean enabledByDefault) {
		super(id, name, category, Corner.TOP_LEFT, enabledByDefault);
	}

	protected abstract String text(Minecraft mc);

	private String shown(Minecraft mc, boolean preview) {
		String t = preview && mc.player == null ? sample() : text(mc);
		if (t.isEmpty()) return t;
		return switch (brackets.index()) {
			case 1 -> "[" + t + "]";
			case 2 -> "(" + t + ")";
			case 3 -> "<" + t + ">";
			default -> t;
		};
	}

	@Override
	public boolean visible(Minecraft mc) {
		return !text(mc).isEmpty();
	}

	@Override
	public float scale() {
		return size.value() / 100F;
	}

	@Override
	public int width(Minecraft mc) {
		return mc.font.width(shown(mc, true)) + 6;
	}

	@Override
	public int height(Minecraft mc) {
		return mc.font.lineHeight + 4;
	}

	public int colorAt(int i, int length, long now) {
		float speed = animSpeed.value() / 100F;
		float sat = rainbowSaturation.value() / 100F;
		float along = length > 1 ? i / (float) (length - 1) : 0;
		switch (colorMode.index()) {
			case 1:
				return ARGB.srgbLerp(along, color.argb(), color2.argb());
			case 2:
				return 0xFF000000 | Mth.hsvToRgb(((now * speed / 20 + i * 12) % 360) / 360F, sat, 1F);
			case 3: {
				float t = (along * 0.5F + now * speed / 2000F) % 1F;
				return ARGB.srgbLerp(t < 0.5F ? t * 2 : (1 - t) * 2, color.argb(), color2.argb());
			}
			case 4:
				return ARGB.srgbLerp((float) (Math.sin(now * speed / 1500.0 * Math.PI * 2) * 0.5 + 0.5), color.argb(), color2.argb());
			case 5:
				return 0xFF000000 | Mth.hsvToRgb(((now * speed / 20) % 360) / 360F, sat, 1F);
			default:
				return color.argb();
		}
	}

	@Override
	public void render(Gfx g, Minecraft mc, int x, int y) {
		String s = shown(mc, true);
		int w = width(mc), h = height(mc);
		long now = System.currentTimeMillis();
		if (background.on()) {
			int a = Math.round(opacity.value() * 2.55F);
			g.fill(x, y, x + w, y + h, a << 24 | backgroundColor.argb() & 0xFFFFFF);
		}
		int line = colorAt(0, s.length(), now);
		switch (accentLine.index()) {
			case 1: g.fill(x, y, x + 1, y + h, line); break;
			case 2: g.fill(x, y + h - 1, x + w, y + h, line); break;
			case 3:
				g.fill(x - 1, y - 1, x + w + 1, y, line);
				g.fill(x - 1, y + h, x + w + 1, y + h + 1, line);
				g.fill(x - 1, y, x, y + h, line);
				g.fill(x + w, y, x + w + 1, y + h, line);
				break;
			default: break;
		}
		int cx = x + 3;
		for (int i = 0; i < s.length(); i++) {
			String ch = String.valueOf(s.charAt(i));
			g.drawString(mc.font, ch, cx, y + 2, colorAt(i, s.length(), now), shadow.on());
			cx += mc.font.width(ch);
		}
	}
}
