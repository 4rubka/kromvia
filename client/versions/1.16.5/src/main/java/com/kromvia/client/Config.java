package com.kromvia.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kromvia.client.module.setting.Setting;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.kromvia.client.cosmetic.Cosmetic;
import com.kromvia.client.cosmetic.Wardrobe;
import com.kromvia.client.module.Module;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public final class Config {
	private static final Logger LOG = LogManager.getLogger("kromvia");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("kromvia.json");

	private static final class Data {
		Map<String, Boolean> modules = new TreeMap<>();
		Map<String, Integer> keys = new TreeMap<>();
		Map<String, JsonObject> settings = new TreeMap<>();
		Map<Cosmetic.Slot, String> cosmetics = new TreeMap<>();
		Map<String, Integer> colors = new TreeMap<>();
		Map<String, float[]> hud = new TreeMap<>();
	}

	private Config() {}

	public static void load() {
		if (!Files.isRegularFile(FILE)) return;
		try {
			Data data = GSON.fromJson(new String(Files.readAllBytes(FILE), java.nio.charset.StandardCharsets.UTF_8), Data.class);
			if (data == null) return;
			for (Module m : KromviaClient.MODULES) {
				Boolean on = data.modules == null ? null : data.modules.get(m.id);
				if (on != null) m.enabled = on;
				Integer key = data.keys == null ? null : data.keys.get(m.id);
				m.key = key == null ? -1 : key;
				float[] pos = data.hud == null ? null : data.hud.get(m.id);
				if (pos != null && pos.length == 2) {
					m.hudX = pos[0];
					m.hudY = pos[1];
				}
				JsonObject values = data.settings == null ? null : data.settings.get(m.id);
				if (values == null) continue;
				for (Setting<?> st : m.settings) {
					JsonElement v = values.get(st.id);
					if (v != null) st.load(v);
				}
			}
			if (data.cosmetics != null) Wardrobe.EQUIPPED.putAll(data.cosmetics);
			if (data.colors != null) Wardrobe.COLORS.putAll(data.colors);
		} catch (IOException | JsonParseException e) {
			LOG.warn("Could not read {}, using defaults", FILE, e);
		}
	}

	public static void save() {
		Data data = new Data();
		for (Module m : KromviaClient.MODULES) {
			data.modules.put(m.id, m.enabled);
			if (m.key >= 0) data.keys.put(m.id, m.key);
			if (m.hudX >= 0) data.hud.put(m.id, new float[] {m.hudX, m.hudY});
			if (m.settings.isEmpty()) continue;
			JsonObject values = new JsonObject();
			for (Setting<?> st : m.settings) values.add(st.id, st.save());
			data.settings.put(m.id, values);
		}
		data.cosmetics.putAll(Wardrobe.EQUIPPED);
		data.colors.putAll(Wardrobe.COLORS);
		try {
			Files.createDirectories(FILE.getParent());
			Files.write(FILE, GSON.toJson(data).getBytes(java.nio.charset.StandardCharsets.UTF_8));
		} catch (IOException e) {
			LOG.warn("Could not write {}", FILE, e);
		}
	}

	private static final Path PROFILES = System.getenv("KROMVIA_PROFILES") != null
			? java.nio.file.Paths.get(System.getenv("KROMVIA_PROFILES"))
			: FabricLoader.getInstance().getConfigDir().resolve("kromvia-profiles");

	public static boolean validProfile(String name) {
		return name.matches("[A-Za-z0-9 _-]{1,32}");
	}

	public static java.util.List<String> profiles() {
		java.util.List<String> out = new java.util.ArrayList<>();
		try (java.util.stream.Stream<Path> files = Files.list(PROFILES)) {
			files.forEach(p -> {
				String n = p.getFileName().toString();
				if (n.endsWith(".json")) out.add(n.substring(0, n.length() - 5));
			});
		} catch (IOException ignored) {
		}
		java.util.Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
		return out;
	}

	public static void saveProfile(String name) {
		if (!validProfile(name)) return;
		save();
		try {
			Files.createDirectories(PROFILES);
			Files.copy(FILE, PROFILES.resolve(name + ".json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			LOG.warn("Could not save profile {}", name, e);
		}
	}

	public static void loadProfile(String name) {
		if (!validProfile(name)) return;
		try {
			Files.createDirectories(FILE.getParent());
			Files.copy(PROFILES.resolve(name + ".json"), FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			LOG.warn("Could not load profile {}", name, e);
			return;
		}
		load();
	}

	public static void deleteProfile(String name) {
		if (!validProfile(name)) return;
		try {
			Files.deleteIfExists(PROFILES.resolve(name + ".json"));
		} catch (IOException e) {
			LOG.warn("Could not delete profile {}", name, e);
		}
	}
}
