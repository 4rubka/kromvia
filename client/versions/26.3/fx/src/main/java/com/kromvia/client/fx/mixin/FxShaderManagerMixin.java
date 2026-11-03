package com.kromvia.client.fx.mixin;

import com.kromvia.client.fx.Assets;
import java.util.Map;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShaderManager.class)
public class FxShaderManagerMixin {
	@Redirect(method = "loadConfigs", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/packs/resources/ResourceManager;listResources(Ljava/lang/String;Lnet/minecraft/server/packs/resources/ResourceManager$Selector;)Ljava/util/Map;"))
	private static Map<Identifier, Resource> kromvia$shaders(ResourceManager manager, String dir, ResourceManager.Selector filter) {
		return Assets.with(manager.listResources(dir, filter), dir, manager);
	}

	@Redirect(method = "loadConfigs", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/resources/FileToIdConverter;listMatchingResources(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;"))
	private static Map<Identifier, Resource> kromvia$postEffects(FileToIdConverter files, ResourceManager manager) {
		return Assets.with(files.listMatchingResources(manager), "post_effect", manager);
	}
}
