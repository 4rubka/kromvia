package com.kromvia.client.module;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kromvia.client.module.setting.BoolSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

public final class IrlWeather extends Module {
	private static final long EVERY_MS = 15 * 60_000L;
	private static final int[] THUNDER = {200, 386, 389, 392, 395};
	private static final int[] DRY = {113, 116, 119, 122, 143, 248, 260};

	public static final IrlWeather INSTANCE = new IrlWeather();

	private final BoolSetting storms = add(new BoolSetting("storms", "Thunderstorms (wttr.in sees your IP)", true));
	private volatile float rain = -1;
	private volatile float thunder;
	private volatile long fetched;
	private volatile boolean busy;

	private IrlWeather() {
		super("irl_weather", "IRL Weather", Category.UTILITY, Corner.TOP_LEFT, false);
		icon(Items.SUNFLOWER);
	}

	@Override
	public boolean hud() {
		return false;
	}

	@Override
	public void tick(Minecraft mc) {
		long now = System.currentTimeMillis();
		if (busy || now - fetched < EVERY_MS) return;
		busy = true;
		fetched = now;
		Thread t = new Thread(this::fetch, "Kromvia IRL weather");
		t.setDaemon(true);
		t.start();
	}

	private void fetch() {
		try {
			HttpURLConnection c = (HttpURLConnection) new URL("https://wttr.in/?format=j1").openConnection();
			c.setConnectTimeout(5000);
			c.setReadTimeout(8000);
			c.setRequestProperty("User-Agent", "Kromvia");
			try (InputStream in = c.getInputStream()) {
				JsonObject o = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
				int code = o.getAsJsonArray("current_condition").get(0).getAsJsonObject().get("weatherCode").getAsInt();
				boolean storm = contains(THUNDER, code);
				rain = storm || !contains(DRY, code) ? 1 : 0;
				thunder = storm ? 1 : 0;
			}
		} catch (Exception e) {
			fetched -= EVERY_MS - 60_000L;
		} finally {
			busy = false;
		}
	}

	private static boolean contains(int[] codes, int code) {
		for (int c : codes) {
			if (c == code) return true;
		}
		return false;
	}

	public float rain() {
		return enabled ? rain : -1;
	}

	public float thunder() {
		return enabled && rain >= 0 ? storms.on() ? thunder : 0 : -1;
	}
}
