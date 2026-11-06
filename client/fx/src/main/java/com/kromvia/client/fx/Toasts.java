package com.kromvia.client.fx;

import com.kromvia.client.Accounts;
import com.kromvia.client.PlayTime;
import com.kromvia.client.Social;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Toasts {
	private static final SystemToast.SystemToastId ID = new SystemToast.SystemToastId(5000L);
	private static final int MAX_TEXT = 200;

	private static final Logger PRESENCE = LoggerFactory.getLogger("kromvia-presence");

	private Toasts() {}

	static void listen() {
		Accounts.sender = PRESENCE::info;
		Thread t = new Thread(Toasts::read, "Kromvia launcher toasts");
		t.setDaemon(true);
		t.start();
	}

	private static void read() {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
			for (String line; (line = in.readLine()) != null; ) {
				try {
					JsonObject msg = JsonParser.parseString(line).getAsJsonObject();
					Minecraft mc = Minecraft.getInstance();
					JsonObject social = msg.getAsJsonObject("social");
					if (social != null) {
						Social.set(social);
						continue;
					}
					JsonObject accounts = msg.getAsJsonObject("accounts");
					if (accounts != null) {
						Accounts.set(accounts);
						continue;
					}
					if (msg.has("wardrobe") && mc != null) {
						mc.execute(com.kromvia.client.cosmetic.Wardrobe::loadShared);
						continue;
					}
					if (msg.has("media")) {
						Media.set(msg.get("media").isJsonNull() ? null : msg.getAsJsonObject("media"));
						continue;
					}
					JsonObject played = msg.getAsJsonObject("playtime");
					if (played != null) {
						PlayTime.set(played.get("build").getAsLong(), played.get("total").getAsLong());
						continue;
					}
					JsonObject shared = msg.getAsJsonObject("shared");
					if (shared != null && mc != null) {
						String url = shared.has("url") ? shared.get("url").getAsString() : null;
						String error = shared.has("error") ? shared.get("error").getAsString() : "";
						mc.execute(() -> Qol.shared(mc, url, error));
						continue;
					}
					JsonObject worn = msg.getAsJsonObject("worn");
					if (worn != null && mc != null) {
						String host = worn.get("host").isJsonNull() ? null : worn.get("host").getAsString();
						JsonObject found = worn.getAsJsonObject("worn");
						mc.execute(() -> Players.answer(host, found));
						continue;
					}
					JsonObject toast = msg.getAsJsonObject("toast");
					if (toast == null) continue;
					String title = toast.get("title").getAsString();
					String text = toast.get("text").getAsString();
					String shown = text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) + "…" : text;
					if (mc != null) mc.execute(() -> Types.toast(mc, ID, Component.literal(title), Component.literal(shown)));
				} catch (RuntimeException ignored) {
				}
			}
		} catch (IOException ignored) {
		}
	}
}
