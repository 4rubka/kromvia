package com.kromvia.client.ui;

import com.kromvia.client.compat.Mc;
import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.Reload;
import java.util.function.Supplier;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PlayerPreview {
	public static final int ID = Integer.MIN_VALUE + 7;
	private static Supplier<PlayerSkin> skin;

	private PlayerPreview() {}

	public static PlayerSkin skin() {
		if (skin == null) skin = Mc.ownSkin();
		return skin.get();
	}

	public static void render(Gfx g, int x1, int y1, int x2, int y2, float yaw, float ageTicks) {
		// Entity/block models aren't ready during the initial reload.
		if (Reload.active()) return;
		AvatarRenderState s = new AvatarRenderState();
		s.entityType = Mc.playerType();
		s.skin = skin();
		s.id = ID;
		s.bodyRot = yaw;
		s.yRot = 0;
		s.xRot = 0;
		s.ageInTicks = ageTicks;
		Mc.fullBright(s);
		s.boundingBoxWidth = 0.6F;
		s.boundingBoxHeight = 1.8F;
		float scale = Math.min((y2 - y1) / 2.7F, (x2 - x1) / 2.4F);
		Vector3f offset = new Vector3f(0, 1.2F, 0);
		g.entity(s, scale, offset, new Quaternionf().rotateZ((float) Math.PI), new Quaternionf(), x1, y1, x2, y2);
	}
}
