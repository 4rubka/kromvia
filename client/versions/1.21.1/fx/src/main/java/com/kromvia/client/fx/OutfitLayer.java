package com.kromvia.client.fx;

import com.kromvia.client.ui.PlayerPreview;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import com.kromvia.client.compat.PlayerRenderState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutfitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private static final Logger LOG = LoggerFactory.getLogger("kromvia");

	private static final class Worn {
		final Glb glb;
		final List<Outfit.Piece> pieces;
		final ResourceLocation[] textures;
		private final boolean[] uploaded;

		Worn(String id, Glb glb) {
			this.glb = glb;
			this.pieces = Outfit.pieces(id, glb);
			textures = new ResourceLocation[glb.images.size()];
			uploaded = new boolean[textures.length];
			for (int m = 0; m < textures.length; m++) {
				textures[m] = ResourceLocation.fromNamespaceAndPath("kromvia", "outfit/" + id + "/" + m);
			}
		}

		ResourceLocation texture(int material) {
			if (material < 0 || material >= textures.length) return white();
			byte[] png = glb.images.get(material);
			if (png == null) return white();
			if (!uploaded[material]) {
				uploaded[material] = true;
				try {
					Minecraft.getInstance().getTextureManager().register(textures[material], Chat.texture(textures[material].toString(), NativeImage.read(new java.io.ByteArrayInputStream(png))));
				} catch (Exception e) {
					LOG.warn("Outfit texture {} failed to load", textures[material], e);
					glb.images.set(material, null);
					return white();
				}
			}
			return textures[material];
		}
	}

	private static ResourceLocation white;

	private static ResourceLocation white() {
		if (white == null) {
			white = ResourceLocation.fromNamespaceAndPath("kromvia", "outfit/white");
			NativeImage img = new NativeImage(1, 1, false);
			Ver.setPixel(img, 0, 0, 0xFFFFFFFF);
			Minecraft.getInstance().getTextureManager().register(white, Chat.texture(white.toString(), img));
		}
		return white;
	}

	private static final Map<String, Worn> LOADED = new HashMap<>();

	private static Worn load(String id) {
		if (LOADED.containsKey(id)) return LOADED.get(id);
		Worn w = null;
		if (Outfit.known(id)) {
			try (InputStream in = OutfitLayer.class.getResourceAsStream("/assets/kromvia/models/" + Outfit.file(id) + ".glb")) {
				if (in == null) throw new IllegalStateException("not bundled");
				w = new Worn(id, Glb.read(in.readAllBytes()));
			} catch (Exception e) {
				LOG.warn("Outfit model {} failed to load", id, e);
			}
		}
		LOADED.put(id, w);
		return w;
	}

	private static List<String> ids(Minecraft mc, PlayerRenderState state) {
		if (state.id == PlayerPreview.ID || mc.player != null && state.id == mc.player.getId()) return com.kromvia.client.cosmetic.Wardrobe.models();
		if (mc.level == null || !(mc.level.getEntity(state.id) instanceof Player p)) return null;
		return Players.worn(p.getGameProfile().getName());
	}

	private ModelPart part(Outfit.Part part) {
		var m = getParentModel();
		return switch (part) {
			case HEAD -> m.head;
			case BODY -> m.body;
			case RIGHT_ARM -> m.rightArm;
			case LEFT_ARM -> m.leftArm;
			case RIGHT_LEG -> m.rightLeg;
			case LEFT_LEG -> m.leftLeg;
		};
	}

	public OutfitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource out, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float delta, float ageInTicks, float yRot, float xRot) {
		PlayerRenderState state = new PlayerRenderState();
		state.id = player.getId();
		state.ageInTicks = ageInTicks;
		state.isInvisible = player.isInvisible();
		state.headItem = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
		state.chestItem = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
		draw(pose, out, light, state);
	}

	public void draw(PoseStack pose, MultiBufferSource out, int light, PlayerRenderState state) {
		Minecraft mc = Minecraft.getInstance();
		List<String> ids = state.isInvisible ? null : ids(mc, state);
		if (ids == null) return;
		for (String id : ids) {
			Worn w = load(id);
			if (w == null) continue;
			if (Outfit.wings(id) && state.chestItem.is(Items.ELYTRA)) continue;
			Matrix4f[] nodes = w.glb.pose(state.ageInTicks / 20);
			for (Outfit.Piece piece : w.pieces) {
				if (piece.part() == Outfit.Part.HEAD && !state.headItem.isEmpty()) continue;
				pose.pushPose();
				part(piece.part()).translateAndRotate(pose);
				pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
				Types.mulPose(pose, piece.place());
				for (Map.Entry<Integer, List<int[]>> e : piece.draws().entrySet()) {
					int color = e.getKey() >= 0 ? w.glb.colors.get(e.getKey()) : 0xFFFFFFFF;
					List<int[]> draws = e.getValue();
					{
						VertexConsumer b = out.getBuffer(Types.model(w.texture(e.getKey())));
						PoseStack ps = pose;
						for (int[] d : draws) {
							ps.pushPose();
							Types.mulPose(ps, nodes[d[0]]);
							PoseStack.Pose q = ps.last();
							Glb.Prim prim = w.glb.meshes.get(d[1]).get(d[2]);
							float[] pos = prim.pos(), nor = prim.normal(), uv = prim.uv();
							int[] idx = prim.index();
							for (int t = 0; t + 2 < idx.length; t += 3) {
								for (int c = 0; c < 4; c++) {
									int i = idx[t + Math.min(c, 2)];
									Types.modelVertex(b, q, pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2], color, uv[i * 2], uv[i * 2 + 1], light, nor[i * 3], nor[i * 3 + 1], nor[i * 3 + 2]);
								}
							}
							ps.popPose();
						}
					}
				}
				pose.popPose();
			}
		}
	}
}
