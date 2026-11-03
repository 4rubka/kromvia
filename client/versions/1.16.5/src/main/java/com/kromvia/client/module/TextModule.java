package com.kromvia.client.module;

import com.kromvia.client.compat.Gfx;
import com.kromvia.client.module.setting.BoolSetting;
import com.kromvia.client.module.setting.ChoiceSetting;
import com.kromvia.client.module.setting.ColorSetting;
import com.kromvia.client.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import com.kromvia.client.compat.ARGB;
import net.minecraft.util.Mth;

public abstract class TextModule extends Module {
	static final int PLATE = 0x90000000;

	public final ChoiceSetting colorMode = add(new ChoiceSetting("color_mode", "Colour", 0, "Solid", "Gradient", "Rainbow"));
	public final ColorSetting color = add(new ColorSetting("color", "Main colour", 0xFFFFFFFF));
	public final ColorSetting color2 = add(new ColorSetting("color2", "Gradient end", 0xFF8B5CF6));
	public final BoolSetting background = add(new BoolSetting("background", "Background", true));
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
		switch (brackets.index()) {
			case 1: return "[" + t + "]";
			case 2: return "(" + t + ")";
			case 3: return "<" + t + ">";
			default: return t;
		}
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

	@Override
	public void render(Gfx g, Minecraft mc, int x, int y) {
		String s = shown(mc, true);
		if (background.on()) {
			int a = Math.round(opacity.value() * 2.55F);
			g.fill(x, y, x + width(mc), y + height(mc), ARGB.color(a, 0, 0, 0));
		}
		int cx = x + 3;
		long now = System.currentTimeMillis();
		for (int i = 0; i < s.length(); i++) {
			String ch = String.valueOf(s.charAt(i));
			int c;
			switch (colorMode.index()) {
				case 1: c = ARGB.lerp(s.length() > 1 ? i / (float) (s.length() - 1) : 0, color.argb(), color2.argb()); break;
				case 2: c = 0xFF000000 | Mth.hsvToRgb(((now / 20 + i * 12) % 360) / 360F, 0.65F, 1F); break;
				default: c = color.argb();
			}
			g.drawString(mc.font, ch, cx, y + 2, c, shadow.on());
			cx += mc.font.width(ch);
		}
	}
}
