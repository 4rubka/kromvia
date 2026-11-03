package com.kromvia.client.compat;

import java.util.function.Supplier;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

public record PlayerSkin(ResourceLocation texture, Model model) {
	public enum Model {
		WIDE("default"), SLIM("slim");

		public final String id;

		Model(String id) {
			this.id = id;
		}
	}

	private static volatile PlayerSkin own;

	public static Supplier<PlayerSkin> own() {
		Minecraft mc = Minecraft.getInstance();
		var profile = mc.getUser().getGameProfile();
		own = new PlayerSkin(DefaultPlayerSkin.getDefaultSkin(profile.getId()),
				"slim".equals(DefaultPlayerSkin.getSkinModelName(profile.getId())) ? Model.SLIM : Model.WIDE);
		mc.getSkinManager().registerSkins(profile, (type, texture, info) -> {
			if (type == MinecraftProfileTexture.Type.SKIN) own = new PlayerSkin(texture, "slim".equals(info.getMetadata("model")) ? Model.SLIM : Model.WIDE);
		}, true);
		return () -> own;
	}
}
