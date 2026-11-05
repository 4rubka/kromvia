package com.kromvia.client.fx;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

final class Cover {
	private static final int MAX = 512;
	private String path;
	private CompletableFuture<BufferedImage> source;
	private final Map<Integer, CompletableFuture<BufferedImage>> scaling = new HashMap<>();
	private final Map<Integer, Identifier> textures = new HashMap<>();

	static int pixels(Minecraft mc, int size) {
		return Math.min(MAX, (int) Math.round(size * (double) mc.getWindow().getGuiScale()));
	}

	Identifier get(Minecraft mc, String path, int px) {
		if (path == null || !path.equals(this.path)) {
			for (Identifier id : textures.values()) mc.getTextureManager().release(id);
			textures.clear();
			scaling.clear();
			this.path = path;
			source = path == null ? null : CompletableFuture.supplyAsync(() -> {
				try {
					return ImageIO.read(new File(path));
				} catch (Exception e) {
					return null;
				}
			});
		}
		Identifier id = textures.get(px);
		if (id != null || source == null) return id;
		CompletableFuture<BufferedImage> job = scaling.computeIfAbsent(px, n -> source.thenApplyAsync(img -> img == null ? null : scale(img, n)));
		if (!job.isDone()) return null;
		BufferedImage img = job.getNow(null);
		if (img == null) return null;
		NativeImage n = new NativeImage(px, px, false);
		for (int y = 0; y < px; y++) {
			for (int x = 0; x < px; x++) Ver.setPixel(n, x, y, img.getRGB(x, y));
		}
		id = Identifier.fromNamespaceAndPath("kromvia", "music_cover/" + px + "_" + System.nanoTime());
		mc.getTextureManager().register(id, Chat.texture("kromvia music cover", n));
		textures.put(px, id);
		return id;
	}

	private static BufferedImage scale(BufferedImage img, int px) {
		int side = Math.min(img.getWidth(), img.getHeight());
		BufferedImage cur = img.getSubimage((img.getWidth() - side) / 2, (img.getHeight() - side) / 2, side, side);
		while (side / 2 >= px) {
			side /= 2;
			cur = draw(cur, side);
		}
		return draw(cur, px);
	}

	private static BufferedImage draw(BufferedImage src, int px) {
		BufferedImage out = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(src, 0, 0, px, px, null);
		g.dispose();
		return out;
	}
}
