package com.kromvia.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class Accounts {
	private Accounts() {}

	public static final class Entry {
		private final String id;
		private final String name;

		Entry(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String id() {
			return id;
		}

		public String name() {
			return name;
		}
	}

	public static volatile Consumer<String> sender;
	private static volatile List<Entry> list = Collections.emptyList();
	private static volatile String active;
	private static volatile boolean switching;

	public static void set(JsonObject msg) {
		List<Entry> entries = new ArrayList<>();
		JsonArray all = msg.getAsJsonArray("list");
		if (all != null) {
			for (JsonElement e : all) {
				JsonObject o = e.getAsJsonObject();
				entries.add(new Entry(o.get("id").isJsonNull() ? null : o.get("id").getAsString(), o.get("name").getAsString()));
			}
		}
		active = msg.get("active") == null || msg.get("active").isJsonNull() ? null : msg.get("active").getAsString();
		list = Collections.unmodifiableList(entries);
		com.kromvia.client.cosmetic.Cosmetics.prewarm();
	}

	public static boolean licensed() {
		return active != null;
	}

	public static boolean available() {
		return sender != null && !list.isEmpty();
	}

	public static List<Entry> list() {
		return list;
	}

	public static boolean isActive(Entry e) {
		return java.util.Objects.equals(e.id(), active);
	}

	public static boolean switching() {
		return switching;
	}

	public static void switchTo(Entry entry) {
		Consumer<String> send = sender;
		if (send == null || switching || isActive(entry)) return;
		switching = true;
		JsonObject who = new JsonObject();
		who.addProperty("id", entry.id());
		who.addProperty("name", entry.name());
		JsonObject msg = new JsonObject();
		msg.add("switch", who);
		send.accept(msg.toString());
	}
}
