package com.kromvia.client.fx;

import com.kromvia.client.compat.Mc;
import com.kromvia.client.module.Module;
import com.kromvia.client.module.setting.BoolSetting;
import com.kromvia.client.module.setting.ColorSetting;
import com.kromvia.client.module.setting.SliderSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class Tweaks {
	private Tweaks() {}

	private static class Flag extends Module {
		Flag(String id, String name, Category cat) {
			super(id, name, cat, Corner.TOP_LEFT, false);
		}

		@Override
		public boolean hud() {
			return false;
		}
	}

	public static final class Chat extends Flag {
		public final BoolSetting compact = add(new BoolSetting("compact", "Stack repeats (xN)", true));
		public final BoolSetting keep = add(new BoolSetting("keep", "Keep after leaving", true));
		public final BoolSetting slide = add(new BoolSetting("slide", "New messages slide in", true));
		public final ColorSetting background = add(new ColorSetting("background", "Background colour", 0xFF000000));
		public final SliderSetting strength = add(new SliderSetting("strength", "Background strength", 100, 0, 200, 10, "%"));
		public final SliderSetting corners = add(new SliderSetting("corners", "Rounded corners", 0, 0, 6, 1, "px"));

		Chat() {
			super("better_chat", "Better Chat", Category.UTILITY);
			icon(Items.BOOK);
			corners.shown = false;
			slide.shown = false;
		}
	}

	public static final Chat CHAT = new Chat();

	public static final class Scoreboard extends Flag {
		public final BoolSetting numbers = add(new BoolSetting("numbers", "Red numbers", false));
		public final SliderSetting background = add(new SliderSetting("background", "Background", 100, 0, 200, 10, "%"));
		public final SliderSetting scale = add(new SliderSetting("scale", "Size", 1, 0.5F, 1.5F, 0.05F, "x"));
		public final SliderSetting offset = add(new SliderSetting("offset", "Move up or down", 0, -120, 120, 5, "px"));

		Scoreboard() {
			super("scoreboard", "Scoreboard", Category.HUD);
			icon(Items.OAK_SIGN);
			shown = false;
		}

		public int background(int argb) {
			if (!enabled) return argb;
			int a = Math.min(255, Math.round((argb >>> 24) * background.value() / 100));
			return a << 24 | argb & 0xFFFFFF;
		}
	}

	public static final Scoreboard SCOREBOARD = new Scoreboard();

	public static final class VanillaHud extends Flag {
		public final BoolSetting title = add(new BoolSetting("title", "Show titles", true));
		public final SliderSetting titleScale = add(new SliderSetting("title_scale", "Title size", 1, 0.3F, 1.5F, 0.05F, "x"));
		public final BoolSetting actionbar = add(new BoolSetting("actionbar", "Show the action bar", true));
		public final SliderSetting actionbarScale = add(new SliderSetting("actionbar_scale", "Action bar size", 1, 0.5F, 2, 0.05F, "x"));
		public final SliderSetting actionbarOffset = add(new SliderSetting("actionbar_offset", "Action bar height", 0, -150, 50, 5, "px"));
		public final BoolSetting bossbar = add(new BoolSetting("bossbar", "Show boss bars", true));
		public final SliderSetting bossbarScale = add(new SliderSetting("bossbar_scale", "Boss bar size", 1, 0.5F, 1.5F, 0.05F, "x"));

		VanillaHud() {
			super("vanilla_hud", "Vanilla HUD", Category.HUD);
			icon(Items.ITEM_FRAME);
			shown = false;
		}
	}

	public static final VanillaHud VANILLA_HUD = new VanillaHud();

	public static final class Glint extends Flag {
		private static final String[] TEXTURES = {"textures/misc/enchanted_glint_item.png", "textures/misc/enchanted_glint_armor.png", "textures/misc/enchanted_item_glint.png"};
		private static final int SIZE = 64;
		public final ColorSetting colour = add(new ColorSetting("colour", "Glint colour", 0xFF8B5CF6));
		private Integer applied;

		Glint() {
			super("glint", "Glint Colour", Category.UTILITY);
			icon(Items.ENCHANTED_BOOK);
		}

		@Override
		public void tick(Minecraft mc) {
			int argb = colour.argb();
			if (applied != null && applied == argb) return;
			applied = argb;
			for (String path : TEXTURES) {
				com.mojang.blaze3d.platform.NativeImage image = new com.mojang.blaze3d.platform.NativeImage(SIZE, SIZE, true);
				for (int y = 0; y < SIZE; y++) {
					for (int x = 0; x < SIZE; x++) {
						double a = (Math.sin((x * 2 + y) * Math.PI / 16) + 1) / 2;
						double b = (Math.sin((x - y * 2) * Math.PI / 32) + 1) / 2;
						double v = Math.pow(a * 0.6 + b * 0.4, 2);
						int r = (int) ((argb >> 16 & 0xFF) * v), g = (int) ((argb >> 8 & 0xFF) * v), bl = (int) ((argb & 0xFF) * v);
						Ver.setPixel(image, x, y, 0xFF000000 | r << 16 | g << 8 | bl);
					}
				}
				Identifier id = Identifier.fromNamespaceAndPath("minecraft", path);
				mc.getTextureManager().register(id, com.kromvia.client.fx.Chat.texture("kromvia glint", image));
			}
		}

		@Override
		public void disable(Minecraft mc) {
			if (applied == null) return;
			applied = null;
			for (String path : TEXTURES) mc.getTextureManager().release(Identifier.fromNamespaceAndPath("minecraft", path));
		}
	}

	public static final Glint GLINT = new Glint();

	private static String lastLine;
	private static int repeats;

	private static String key(Component message) {
		return message.getString().replaceFirst("^\\[\\d\\d:\\d\\d] ", "");
	}

	public static boolean repeats(Component message) {
		String k = key(message);
		boolean same = CHAT.enabled && CHAT.compact.on() && k.equals(lastLine);
		repeats = same ? repeats + 1 : 1;
		lastLine = k;
		if (!same) arrived = System.currentTimeMillis();
		return same;
	}

	private static long arrived;
	private static final float SLIDE_MS = 160;

	public static float chatSlide() {
		if (!CHAT.enabled || !CHAT.slide.on()) return 0;
		float t = (System.currentTimeMillis() - arrived) / SLIDE_MS;
		if (t >= 1) return 0;
		float left = 1 - t;
		return 9 * left * left * left;
	}

	public static Component counted(Component message) {
		return message.copy().append(Component.literal(" (x" + repeats + ")").withStyle(s -> s.withColor(0xFF8B8B8B)));
	}

	public static int chatBackground(int argb) {
		if (!CHAT.enabled || (argb & 0xFFFFFF) != 0) return argb;
		int alpha = Math.min(255, Math.round((argb >>> 24) * CHAT.strength.value() / 100));
		return alpha << 24 | CHAT.background.argb() & 0xFFFFFF;
	}

	public interface Fill {
		void fill(int x1, int y1, int x2, int y2, int argb);
	}

	private static final List<int[]> chatRows = new java.util.ArrayList<>();
	private static Fill chatFill;

	public static void roundedChat() {
		CHAT.corners.shown = true;
	}

	public static boolean chatRow(Fill fill, int x1, int y1, int x2, int y2, int argb) {
		if (!CHAT.enabled || CHAT.corners.value() < 1 || (argb & 0xFFFFFF) != 0) return false;
		chatFill = fill;
		chatRows.add(new int[] {Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), chatBackground(argb)});
		return true;
	}

	public static void flushChat() {
		if (chatRows.isEmpty()) return;
		int top = Integer.MAX_VALUE, bottom = Integer.MIN_VALUE;
		for (int[] r : chatRows) {
			top = Math.min(top, r[1]);
			bottom = Math.max(bottom, r[3]);
		}
		int radius = Math.round(CHAT.corners.value());
		for (int[] r : chatRows) {
			int y1 = r[1], y2 = r[3];
			int rt = r[1] == top ? Math.min(radius, (y2 - y1) / (r[3] == bottom ? 2 : 1)) : 0;
			int rb = r[3] == bottom ? Math.min(radius, (y2 - y1) / (r[1] == top ? 2 : 1)) : 0;
			for (int i = 0; i < rt; i++) {
				int in = inset(rt, i);
				chatFill.fill(r[0] + in, y1 + i, r[2] - in, y1 + i + 1, r[4]);
			}
			for (int i = 0; i < rb; i++) {
				int in = inset(rb, i);
				chatFill.fill(r[0] + in, y2 - i - 1, r[2] - in, y2 - i, r[4]);
			}
			if (y2 - rb > y1 + rt) chatFill.fill(r[0], y1 + rt, r[2], y2 - rb, r[4]);
		}
		chatRows.clear();
		chatFill = null;
	}

	private static int inset(int r, int i) {
		double d = r - i - 0.5;
		return (int) Math.round(r - Math.sqrt(r * r - d * d));
	}

	public static boolean keepChat() {
		return CHAT.enabled && CHAT.keep.on();
	}

	public static final class Nametags extends Flag {
		public final BoolSetting own = add(new BoolSetting("own", "Own in third person", true));
		public final SliderSetting background = add(new SliderSetting("background", "Background", 25, 0, 100, 5, "%"));
		public final SliderSetting scale = add(new SliderSetting("scale", "Size", 1, 0.5F, 2, 0.1F, "x"));
		public final BoolSetting hideName = add(new BoolSetting("hide_name", "Nick Hider (\"You\")", false));

		Nametags() {
			super("nametags", "Nametags", Category.UTILITY);
			icon(Items.NAME_TAG);
		}

		public float background() {
			return background.value() / 100;
		}
	}

	public static final Nametags NAMETAGS = new Nametags();

	public static boolean ownNametag(Minecraft mc) {
		return NAMETAGS.enabled && NAMETAGS.own.on() && !mc.options.getCameraType().isFirstPerson() && !Mc.hudHidden();
	}

	public static Component hideName(Component text) {
		Minecraft mc = Minecraft.getInstance();
		if (!NAMETAGS.enabled || !NAMETAGS.hideName.on() || mc.player == null) return text;
		String name = mc.player.getGameProfile().name();
		if (name.isEmpty() || !text.getString().contains(name)) return text;
		MutableComponent out = Component.empty();
		text.visit((style, part) -> {
			out.append(Component.literal(part.replace(name, "You")).setStyle(style));
			return Optional.empty();
		}, Style.EMPTY);
		return out;
	}

	public static final class Overlays extends Flag {
		public final SliderSetting fire = add(new SliderSetting("fire", "Fire opacity", 90, 10, 100, 5, "%"));
		public final SliderSetting fireHeight = add(new SliderSetting("fire_height", "Fire height", 100, 0, 100, 5, "%"));
		public final SliderSetting shieldHeight = add(new SliderSetting("shield_height", "Shield height", 100, 0, 100, 5, "%"));
		public final BoolSetting water = add(new BoolSetting("water", "Hide underwater tint", false));
		public final BoolSetting portal = add(new BoolSetting("portal", "Hide portal swirl", true));
		public final BoolSetting spyglass = add(new BoolSetting("spyglass", "Hide spyglass scope", false));
		public final SliderSetting mount = add(new SliderSetting("mount", "Mount opacity (first person)", 100, 0, 100, 5, "%"));

		Overlays() {
			super("overlays", "Overlays", Category.UTILITY);
			icon(Items.MAGMA_CREAM);
			mount.shown = false;
		}

		public void fadesMount() {
			mount.shown = true;
		}

		public boolean fadedMount(net.minecraft.world.entity.Entity e) {
			Minecraft mc = Minecraft.getInstance();
			return enabled && mount.shown && mount.value() < 100 && mc.player != null && e == mc.player.getVehicle() && mc.options.getCameraType().isFirstPerson();
		}

		public int mountColor(int argb) {
			return Math.round((argb >>> 24) * mount.value() / 100) << 24 | argb & 0xFFFFFF;
		}

		public boolean hidesMount() {
			return mount.value() <= 0;
		}

		public int fire(int argb) {
			return Math.round(fire((argb >>> 24) / 255F) * 255) << 24 | argb & 0xFFFFFF;
		}

		public float fire(float alpha) {
			return enabled ? fire.value() / 100 : alpha;
		}

		public boolean hides(BoolSetting s) {
			return enabled && s.on();
		}

		public float fireY(float y) {
			return enabled ? y - (1 - fireHeight.value() / 100) * 0.5F : y;
		}

		public void lowerShield(PoseStack pose, ItemStack stack) {
			if (enabled && stack.getItem() == Items.SHIELD) pose.translate(0, -(1 - shieldHeight.value() / 100) * 0.4F, 0);
		}
	}

	public static final Overlays OVERLAYS = new Overlays();

	public static final class Camera extends Flag {
		public final SliderSetting distance = add(new SliderSetting("distance", "Third person distance", 4, 1, 12, 0.5F, "m"));
		public final BoolSetting skipFront = add(new BoolSetting("skip_front", "F5 skips front view", false));
		public final BoolSetting speedFov = add(new BoolSetting("speed_fov", "FOV: sprint and speed", true));
		public final BoolSetting flyFov = add(new BoolSetting("fly_fov", "FOV: flying", true));
		public final BoolSetting bowFov = add(new BoolSetting("bow_fov", "FOV: drawing a bow", true));

		Camera() {
			super("camera", "Camera", Category.UTILITY);
			icon(Items.ENDER_EYE);
		}
	}

	public static final Camera CAMERA = new Camera();

	public static final Module STRETCHED = new Flag("stretched", "Stretched 4:3", Module.Category.PVP).icon(Items.PAINTING);

	public static int projectionWidth(int width) {
		return STRETCHED.enabled ? Math.round(Minecraft.getInstance().getWindow().getHeight() * 4 / 3F) : width;
	}

	public static float cameraDistance(float vanilla) {
		return CAMERA.enabled ? vanilla * CAMERA.distance.value() / 4 : vanilla;
	}

	public static CameraType cycled(CameraType next) {
		return CAMERA.enabled && CAMERA.skipFront.on() && next == CameraType.THIRD_PERSON_FRONT ? CameraType.FIRST_PERSON : next;
	}

	public static Float fov(net.minecraft.client.player.AbstractClientPlayer p, boolean firstPerson, float effectScale) {
		if (!CAMERA.enabled) return null;
		float m = 1;
		if (CAMERA.flyFov.on() && p.getAbilities().flying) m *= 1.1F;
		float walk = p.getAbilities().getWalkingSpeed();
		if (CAMERA.speedFov.on() && walk != 0) m *= ((float) p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) / walk + 1) / 2;
		if (p.isUsingItem()) {
			if (p.getUseItem().is(Items.BOW)) {
				if (CAMERA.bowFov.on()) m *= 1 - Mth.square(Math.min(p.getTicksUsingItem() / 20F, 1)) * 0.15F;
			} else if (firstPerson && p.isScoping()) {
				return 0.1F;
			}
		}
		return Mth.lerp(effectScale, 1, m);
	}

	public static final class Particles extends Flag {
		public final SliderSetting amount = add(new SliderSetting("amount", "Amount", 100, 0, 100, 5, "%"));
		public final BoolSetting blocks = add(new BoolSetting("blocks", "Hide block breaking", false));
		public final BoolSetting explosions = add(new BoolSetting("explosions", "Hide explosions", false));

		Particles() {
			super("particles", "Particles", Category.UTILITY);
			icon(Items.GLOWSTONE_DUST);
		}
	}

	public static final Particles PARTICLES = new Particles();

	public static boolean dropParticle(boolean block, boolean explosion) {
		if (!PARTICLES.enabled) return false;
		if (block && PARTICLES.blocks.on() || explosion && PARTICLES.explosions.on()) return true;
		return Math.random() * 100 >= PARTICLES.amount.value();
	}

	public static final class Weather extends Flag {
		public final SliderSetting rain = add(new SliderSetting("rain", "Rain and snow", 100, 0, 100, 5, "%"));
		public final BoolSetting lightning = add(new BoolSetting("lightning", "Hide lightning", false));
		public final BoolSetting fishingLine = add(new BoolSetting("fishing_line", "Hide fishing line", false));

		Weather() {
			super("weather", "Weather", Category.UTILITY);
			icon(Items.TRIDENT);
		}
	}

	public static final Weather WEATHER = new Weather();

	public static float rain(float vanilla) {
		return WEATHER.enabled ? vanilla * WEATHER.rain.value() / 100 : vanilla;
	}

	public static boolean hides(BoolSetting s) {
		return WEATHER.enabled && s.on();
	}

	public enum SoundGroup {
		EXPLOSIONS("Explosions", "explode", "tnt"),
		HURT("Hurt and deaths", "hurt", "death"),
		EATING("Eating and drinking", ".eat", ".drink", ".burp"),
		STEPS("Footsteps", "step"),
		WEATHER("Rain and thunder", "weather.", "thunder"),
		PORTALS("Portals", "portal"),
		ORBS("XP orbs and levels", "experience_orb", "player.levelup"),
		ANIMALS("Animal noises", ".ambient");

		final String name;
		final String[] words;

		SoundGroup(String name, String... words) {
			this.name = name;
			this.words = words;
		}

		static SoundGroup of(String path) {
			for (SoundGroup g : values()) {
				for (String w : g.words) {
					if (path.contains(w)) return g;
				}
			}
			return null;
		}
	}

	public static final class Sounds extends Flag {
		final Map<SoundGroup, SliderSetting> volume = new EnumMap<>(SoundGroup.class);

		Sounds() {
			super("sounds", "Sound Volumes", Category.UTILITY);
			icon(Items.JUKEBOX);
			for (SoundGroup g : SoundGroup.values()) volume.put(g, add(new SliderSetting(g.name().toLowerCase(java.util.Locale.ROOT), g.name, 100, 0, 100, 5, "%")));
		}
	}

	public static final Sounds SOUNDS = new Sounds();

	public static float volume(SoundInstance sound, float vanilla) {
		if (!SOUNDS.enabled) return vanilla;
		SoundGroup g = SoundGroup.of(sound.getIdentifier().getPath());
		return g == null ? vanilla : vanilla * SOUNDS.volume.get(g).value() / 100;
	}

	public static final Module ARMOR_ALERT = new Flag("armor_alert", "Armour Break Alert", Module.Category.PVP) {
		private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		private static final String[] NAMES = {"Helmet", "Chestplate", "Leggings", "Boots"};
		final SliderSetting below = add(new SliderSetting("below", "Warn at", 10, 5, 50, 5, "%"));
		private final boolean[] low = new boolean[4];

		{
			icon(Items.CHAINMAIL_CHESTPLATE);
		}

		@Override
		public void tick(Minecraft mc) {
			if (mc.player == null) return;
			for (int i = 0; i < SLOTS.length; i++) {
				ItemStack s = mc.player.getItemBySlot(SLOTS[i]);
				boolean now = s.isDamageableItem() && 100F * (s.getMaxDamage() - s.getDamageValue()) / s.getMaxDamage() < below.value();
				if (now && !low[i]) {
					int left = s.getMaxDamage() - s.getDamageValue();
					Mc.say(Component.literal("§2[§aK§2] §c" + NAMES[i] + " §7is about to break: §f" + left + " §7uses left"));
					mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 1.4F, 0.5F));
				}
				low[i] = now;
			}
		}
	};

	public static List<Module> modules() {
		return List.of(CHAT, SCOREBOARD, VANILLA_HUD, GLINT, NAMETAGS, OVERLAYS, CAMERA, PARTICLES, WEATHER, SOUNDS, ARMOR_ALERT, STRETCHED);
	}
}
