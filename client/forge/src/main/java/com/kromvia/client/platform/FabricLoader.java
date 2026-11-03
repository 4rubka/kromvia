package com.kromvia.client.platform;

import java.lang.reflect.Method;
import java.nio.file.Path;

/** Stands in for Fabric Loader's API on Forge and NeoForge; the build relocates calls to it here. */
public interface FabricLoader {
	FabricLoader INSTANCE = new FabricLoader() {};

	static FabricLoader getInstance() {
		return INSTANCE;
	}

	default Path getConfigDir() {
		return Path.of("config").toAbsolutePath();
	}

	default boolean isModLoaded(String id) {
		for (String pkg : new String[] {"net.neoforged.fml", "net.minecraftforge.fml"}) {
			try {
				Class<?> list = Class.forName(pkg + ".ModList");
				Object mods = list.getMethod("get").invoke(null);
				if (mods == null) return false;
				Method loaded = list.getMethod("isLoaded", String.class);
				return (boolean) loaded.invoke(mods, id);
			} catch (ClassNotFoundException e) {
				continue;
			} catch (ReflectiveOperationException e) {
				return false;
			}
		}
		return false;
	}
}
