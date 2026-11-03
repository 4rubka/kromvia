package com.kromvia.client.fx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class Outfit {
	enum Part { HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG }

	record Piece(Part part, Matrix4f place, Map<Integer, List<int[]>> draws) {}

	private record Fit(Part[] parts, float size, float x, float y, float z, float ry) {}

	private record Rig(String file, boolean back, RigPiece... pieces) {}

	private record RigPiece(Part part, String anchor, String... nodes) {}

	private static final float PI = (float) Math.PI;

	private static final Map<String, Fit> FITS = Map.of(
			"horns", new Fit(new Part[] {Part.HEAD}, 22, 0, 10, 0, 0),
			"horns-small", new Fit(new Part[] {Part.HEAD}, 12, 0, 10, 0, 0),
			"wings-angel", new Fit(new Part[] {Part.BODY}, 30, 0, -2, -5, 0),
			"wings-low-poly", new Fit(new Part[] {Part.BODY}, 26, 0, -3, -4, 0),
			"boots-of-swiftness", new Fit(new Part[] {Part.RIGHT_LEG, Part.LEFT_LEG}, 10.5F, 0, -9.6F, 0, PI / 2));

	private static final Map<String, Rig> RIGS = Map.of(
			"santa-hat", new Rig("christmas", false, new RigPiece(Part.HEAD, "Head_22", "group4_31")),
			"antlers", new Rig("christmas", false, new RigPiece(Part.HEAD, "Head_73", "group3_90")),
			"scarf", new Rig("christmas", true, new RigPiece(Part.BODY, "Body_46", "group_53")),
			"mage-robe", new Rig("mage-robe", true,
					new RigPiece(Part.BODY, "cube_1", "cube_2", "cube_3", "cube_4"),
					new RigPiece(Part.LEFT_ARM, "cube_11", "cube_10", "cube_12"),
					new RigPiece(Part.RIGHT_ARM, "cube_14", "cube_15", "cube_16")),
			"mecha-boost", new Rig("mecha-boost", true,
					new RigPiece(Part.LEFT_LEG, "cube_2", "cube_3", "cube_4", "cube_5"),
					new RigPiece(Part.RIGHT_LEG, "cube_7", "cube_8", "cube_9", "cube_10"),
					new RigPiece(Part.LEFT_ARM, "cube_13", "cube_14", "cube_15", "cube_16", "cube_17", "cube_18", "cube_19"),
					new RigPiece(Part.RIGHT_ARM, "cube_22", "cube_23", "cube_24", "cube_25", "cube_26", "cube_27", "cube_28")));

	private static Vector3f centre(Part part) {
		return switch (part) {
			case HEAD -> new Vector3f(0, 4, 0);
			case BODY -> new Vector3f(0, -6, 0);
			case RIGHT_ARM -> new Vector3f(-1, -4, 0);
			case LEFT_ARM -> new Vector3f(1, -4, 0);
			case RIGHT_LEG, LEFT_LEG -> new Vector3f(0, -6, 0);
		};
	}

	static boolean known(String id) {
		return FITS.containsKey(id) || RIGS.containsKey(id);
	}

	static String file(String id) {
		Rig rig = RIGS.get(id);
		return rig != null ? rig.file : id;
	}

	static boolean wings(String id) {
		return id.startsWith("wings-");
	}

	static List<Piece> pieces(String id, Glb glb) {
		Matrix4f[] rest = glb.pose(0);
		List<Piece> out = new ArrayList<>();
		Fit fit = FITS.get(id);
		if (fit != null) {
			Map<Integer, List<int[]>> draws = draws(glb, rest, null);
			Vector3f[] box = bounds(glb, rest, null);
			Vector3f mid = new Vector3f(box[0]).add(box[1]).mul(0.5F), dims = new Vector3f(box[1]).sub(box[0]);
			float k = fit.size / Math.max(Math.max(dims.x, dims.y), Math.max(dims.z, 1e-6F));
			for (int i = 0; i < fit.parts.length; i++) {
				Matrix4f m = up().translate(fit.x, fit.y, fit.z).rotateY(fit.ry);
				if (i > 0) m.scale(-1, 1, 1);
				out.add(new Piece(fit.parts[i], m.scale(k).translate(-mid.x, -mid.y, -mid.z), draws));
			}
			return out;
		}
		Rig rig = RIGS.get(id);
		for (RigPiece p : rig.pieces) {
			Vector3f[] box = bounds(glb, rest, Set.of(p.anchor));
			Vector3f mid = new Vector3f(box[0]).add(box[1]).mul(0.5F), c = centre(p.part);
			Matrix4f m = up().translate(c).rotateY(rig.back ? PI : 0).scale(16).translate(-mid.x, -mid.y, -mid.z);
			out.add(new Piece(p.part, m, draws(glb, rest, Set.of(p.nodes))));
		}
		return out;
	}

	private static Matrix4f up() {
		return new Matrix4f().scale(1, -1, -1);
	}

	private static boolean in(Glb glb, int node, Set<String> under) {
		if (under == null) return true;
		for (int n = node; n >= 0; n = glb.parents[n]) {
			if (under.contains(glb.names.get(n))) return true;
		}
		return false;
	}

	private static Map<Integer, List<int[]>> draws(Glb glb, Matrix4f[] rest, Set<String> under) {
		Map<Integer, List<int[]>> draws = new LinkedHashMap<>();
		for (int node = 0; node < rest.length; node++) {
			int mesh = glb.meshOf(node);
			if (mesh < 0 || rest[node] == null || !in(glb, node, under)) continue;
			List<Glb.Prim> prims = glb.meshes.get(mesh);
			for (int i = 0; i < prims.size(); i++) draws.computeIfAbsent(prims.get(i).material(), k -> new ArrayList<>()).add(new int[] {node, mesh, i});
		}
		return draws;
	}

	private static Vector3f[] bounds(Glb glb, Matrix4f[] rest, Set<String> under) {
		Vector3f min = new Vector3f(Float.MAX_VALUE), max = new Vector3f(-Float.MAX_VALUE), v = new Vector3f();
		for (int node = 0; node < rest.length; node++) {
			int mesh = glb.meshOf(node);
			if (mesh < 0 || rest[node] == null || !in(glb, node, under)) continue;
			for (Glb.Prim p : glb.meshes.get(mesh)) {
				for (int k = 0; k < p.pos().length; k += 3) {
					rest[node].transformPosition(p.pos()[k], p.pos()[k + 1], p.pos()[k + 2], v);
					min.min(v);
					max.max(v);
				}
			}
		}
		return new Vector3f[] {min, max};
	}
}
