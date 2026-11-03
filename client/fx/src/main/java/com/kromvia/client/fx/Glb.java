package com.kromvia.client.fx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class Glb {
	record Prim(float[] pos, float[] normal, float[] uv, int[] index, int material) {}

	private record Node(int mesh, int[] children, Vector3f t, Quaternionf r, Vector3f s, Matrix4f matrix) {}

	private record Channel(int node, String path, float[] times, float[] values, boolean step) {}

	final List<List<Prim>> meshes = new ArrayList<>();
	private final List<Node> nodes = new ArrayList<>();
	final int[] roots;
	final List<String> names = new ArrayList<>();
	int[] parents;
	final List<byte[]> images = new ArrayList<>();
	final List<Integer> colors = new ArrayList<>();
	private final List<Channel> channels = new ArrayList<>();
	private float duration;

	private final JsonArray accessors, views;
	private final ByteBuffer bin;

	private Glb(JsonObject json, ByteBuffer bin) {
		this.bin = bin;
		accessors = json.getAsJsonArray("accessors");
		views = json.getAsJsonArray("bufferViews");

		for (JsonElement m : array(json, "meshes")) {
			List<Prim> prims = new ArrayList<>();
			for (JsonElement el : array(m.getAsJsonObject(), "primitives")) {
				JsonObject p = el.getAsJsonObject();
				if (num(p, "mode", 4) != 4) continue; // triangles only
				JsonObject attr = p.getAsJsonObject("attributes");
				float[] pos = floats(attr.get("POSITION").getAsInt());
				int n = pos.length / 3;
				float[] normal = attr.has("NORMAL") ? floats(attr.get("NORMAL").getAsInt()) : new float[n * 3];
				float[] uv = attr.has("TEXCOORD_0") ? floats(attr.get("TEXCOORD_0").getAsInt()) : new float[n * 2];
				int[] index = p.has("indices") ? ints(p.get("indices").getAsInt()) : sequence(n);
				prims.add(new Prim(pos, normal, uv, index, num(p, "material", -1)));
			}
			meshes.add(prims);
		}

		for (JsonElement el : array(json, "nodes")) {
			JsonObject o = el.getAsJsonObject();
			int[] children = o.has("children") ? toInts(o.getAsJsonArray("children")) : new int[0];
			float[] t = o.has("translation") ? toFloats(o.getAsJsonArray("translation")) : new float[] {0, 0, 0};
			float[] r = o.has("rotation") ? toFloats(o.getAsJsonArray("rotation")) : new float[] {0, 0, 0, 1};
			float[] s = o.has("scale") ? toFloats(o.getAsJsonArray("scale")) : new float[] {1, 1, 1};
			Matrix4f matrix = o.has("matrix") ? new Matrix4f().set(toFloats(o.getAsJsonArray("matrix"))) : null;
			names.add(o.has("name") ? o.get("name").getAsString() : "");
			nodes.add(new Node(num(o, "mesh", -1), children, new Vector3f(t[0], t[1], t[2]), new Quaternionf(r[0], r[1], r[2], r[3]), new Vector3f(s[0], s[1], s[2]), matrix));
		}
		parents = new int[nodes.size()];
		java.util.Arrays.fill(parents, -1);
		for (int i = 0; i < nodes.size(); i++) {
			for (int c : nodes.get(i).children) parents[c] = i;
		}
		JsonArray scenes = array(json, "scenes");
		roots = scenes.isEmpty() ? sequence(nodes.size()) : toInts(scenes.get(num(json, "scene", 0)).getAsJsonObject().getAsJsonArray("nodes"));

		JsonArray textures = array(json, "textures"), images = array(json, "images");
		for (JsonElement el : array(json, "materials")) {
			JsonObject pbr = el.getAsJsonObject().getAsJsonObject("pbrMetallicRoughness");
			byte[] image = null;
			int color = 0xFFFFFFFF;
			if (pbr != null && pbr.has("baseColorTexture")) {
				JsonObject tex = textures.get(pbr.getAsJsonObject("baseColorTexture").get("index").getAsInt()).getAsJsonObject();
				JsonObject img = images.get(tex.get("source").getAsInt()).getAsJsonObject();
				if (img.has("bufferView")) image = bytes(views.get(img.get("bufferView").getAsInt()).getAsJsonObject());
			}
			if (pbr != null && pbr.has("baseColorFactor")) {
				float[] f = toFloats(pbr.getAsJsonArray("baseColorFactor"));
				color = Math.round(f[3] * 255) << 24 | Math.round(f[0] * 255) << 16 | Math.round(f[1] * 255) << 8 | Math.round(f[2] * 255);
			}
			this.images.add(image);
			colors.add(color);
		}

		JsonArray anims = array(json, "animations");
		if (!anims.isEmpty()) {
			JsonObject a = anims.get(0).getAsJsonObject();
			JsonArray samplers = a.getAsJsonArray("samplers");
			for (JsonElement el : a.getAsJsonArray("channels")) {
				JsonObject ch = el.getAsJsonObject(), target = ch.getAsJsonObject("target");
				String path = target.get("path").getAsString();
				if (!target.has("node") || path.equals("weights")) continue;
				JsonObject s = samplers.get(ch.get("sampler").getAsInt()).getAsJsonObject();
				String interp = s.has("interpolation") ? s.get("interpolation").getAsString() : "LINEAR";
				float[] times = floats(s.get("input").getAsInt()), values = floats(s.get("output").getAsInt());
				if (interp.equals("CUBICSPLINE")) values = middleOfThree(values, times.length);
				channels.add(new Channel(target.get("node").getAsInt(), path, times, values, interp.equals("STEP")));
				duration = Math.max(duration, times[times.length - 1]);
			}
		}
	}

	static Glb read(byte[] data) {
		ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		if (buf.getInt(0) != 0x46546C67) throw new IllegalArgumentException("not a .glb");
		JsonObject json = null;
		ByteBuffer bin = null;
		for (int at = 12; at + 8 <= data.length; ) {
			int len = buf.getInt(at), type = buf.getInt(at + 4);
			if (type == 0x4E4F534A) json = JsonParser.parseString(new String(data, at + 8, len, StandardCharsets.UTF_8)).getAsJsonObject();
			else if (type == 0x004E4942) bin = buf.slice(at + 8, len).order(ByteOrder.LITTLE_ENDIAN);
			at += 8 + len;
		}
		if (json == null || bin == null) throw new IllegalArgumentException("a .glb without JSON or binary chunk");
		return new Glb(json, bin);
	}

	int meshOf(int node) {
		return nodes.get(node).mesh;
	}

	Matrix4f[] pose(float seconds) {
		Vector3f[] t = new Vector3f[nodes.size()], s = new Vector3f[nodes.size()];
		Quaternionf[] r = new Quaternionf[nodes.size()];
		for (int i = 0; i < nodes.size(); i++) {
			Node n = nodes.get(i);
			t[i] = new Vector3f(n.t);
			r[i] = new Quaternionf(n.r);
			s[i] = new Vector3f(n.s);
		}
		boolean[] animated = new boolean[nodes.size()];
		float time = duration > 0 ? seconds % duration : 0;
		for (Channel c : channels) {
			animated[c.node] = true;
			switch (c.path) {
				case "translation" -> sample(c, time, 3, t[c.node]);
				case "scale" -> sample(c, time, 3, s[c.node]);
				case "rotation" -> sample(c, time, r[c.node]);
				default -> {}
			}
		}
		Matrix4f[] world = new Matrix4f[nodes.size()];
		for (int root : roots) walk(root, new Matrix4f(), t, r, s, animated, world);
		return world;
	}

	private void walk(int i, Matrix4f parent, Vector3f[] t, Quaternionf[] r, Vector3f[] s, boolean[] animated, Matrix4f[] world) {
		Node n = nodes.get(i);
		Matrix4f local = n.matrix != null && !animated[i] ? n.matrix : new Matrix4f().translationRotateScale(t[i], r[i], s[i]);
		world[i] = new Matrix4f(parent).mul(local);
		for (int c : n.children) walk(c, world[i], t, r, s, animated, world);
	}

	private static int key(Channel c, float time, float[] f) {
		float[] k = c.times;
		if (time <= k[0]) {
			f[0] = 0;
			return 0;
		}
		int i = Arrays.binarySearch(k, time);
		i = i >= 0 ? i : -i - 2;
		if (i >= k.length - 1) {
			f[0] = 0;
			return k.length - 1;
		}
		f[0] = c.step ? 0 : (time - k[i]) / (k[i + 1] - k[i]);
		return i;
	}

	private static void sample(Channel c, float time, int n, Vector3f out) {
		float[] f = new float[1];
		int i = key(c, time, f), j = Math.min(i + 1, c.times.length - 1);
		float[] v = c.values;
		out.set(v[i * n], v[i * n + 1], v[i * n + 2]).lerp(new Vector3f(v[j * n], v[j * n + 1], v[j * n + 2]), f[0]);
	}

	private static void sample(Channel c, float time, Quaternionf out) {
		float[] f = new float[1];
		int i = key(c, time, f), j = Math.min(i + 1, c.times.length - 1);
		float[] v = c.values;
		out.set(v[i * 4], v[i * 4 + 1], v[i * 4 + 2], v[i * 4 + 3]).slerp(new Quaternionf(v[j * 4], v[j * 4 + 1], v[j * 4 + 2], v[j * 4 + 3]), f[0]).normalize();
	}

	private static float[] middleOfThree(float[] v, int keys) {
		int n = v.length / (keys * 3);
		float[] out = new float[keys * n];
		for (int k = 0; k < keys; k++) System.arraycopy(v, (k * 3 + 1) * n, out, k * n, n);
		return out;
	}

	private static int comps(String type) {
		return switch (type) {
			case "SCALAR" -> 1;
			case "VEC2" -> 2;
			case "VEC3" -> 3;
			case "VEC4" -> 4;
			case "MAT4" -> 16;
			default -> throw new IllegalArgumentException("accessor type " + type);
		};
	}

	private float[] floats(int index) {
		JsonObject a = accessors.get(index).getAsJsonObject();
		if (a.get("componentType").getAsInt() != 5126) throw new IllegalArgumentException("only float accessors here");
		int count = a.get("count").getAsInt(), n = comps(a.get("type").getAsString());
		JsonObject view = views.get(a.get("bufferView").getAsInt()).getAsJsonObject();
		int base = num(view, "byteOffset", 0) + num(a, "byteOffset", 0), stride = num(view, "byteStride", n * 4);
		float[] out = new float[count * n];
		for (int i = 0; i < count; i++) {
			for (int c = 0; c < n; c++) out[i * n + c] = bin.getFloat(base + i * stride + c * 4);
		}
		return out;
	}

	private int[] ints(int index) {
		JsonObject a = accessors.get(index).getAsJsonObject();
		int type = a.get("componentType").getAsInt(), count = a.get("count").getAsInt();
		int size = type == 5121 ? 1 : type == 5123 ? 2 : 4;
		JsonObject view = views.get(a.get("bufferView").getAsInt()).getAsJsonObject();
		int base = num(view, "byteOffset", 0) + num(a, "byteOffset", 0), stride = num(view, "byteStride", size);
		int[] out = new int[count];
		for (int i = 0; i < count; i++) {
			int at = base + i * stride;
			out[i] = switch (type) {
				case 5121 -> bin.get(at) & 0xFF;
				case 5123 -> bin.getShort(at) & 0xFFFF;
				case 5125 -> bin.getInt(at);
				default -> throw new IllegalArgumentException("index type " + type);
			};
		}
		return out;
	}

	private byte[] bytes(JsonObject view) {
		byte[] out = new byte[view.get("byteLength").getAsInt()];
		bin.get(num(view, "byteOffset", 0), out);
		return out;
	}

	private static JsonArray array(JsonObject o, String key) {
		return o.has(key) ? o.getAsJsonArray(key) : new JsonArray();
	}

	private static int num(JsonObject o, String key, int fallback) {
		return o.has(key) ? o.get(key).getAsInt() : fallback;
	}

	private static int[] toInts(JsonArray a) {
		int[] out = new int[a.size()];
		for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsInt();
		return out;
	}

	private static float[] toFloats(JsonArray a) {
		float[] out = new float[a.size()];
		for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsFloat();
		return out;
	}

	private static int[] sequence(int n) {
		int[] out = new int[n];
		for (int i = 0; i < n; i++) out[i] = i;
		return out;
	}
}
