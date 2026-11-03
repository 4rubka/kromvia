package com.kromvia.client.ui;

import com.kromvia.client.Accounts;
import com.kromvia.client.PlayTime;
import com.kromvia.client.Rooms;
import com.kromvia.client.Social;
import com.kromvia.client.compat.Mc;
import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.KScreen;
import java.io.File;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.format.DateTimeFormatter;

import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

public class KromviaTitleScreen extends KScreen {
	private static final int BAR = 26;
	private static final int PANEL_W = 220;
	private static final int BUTTON_W = 180;
	private static final int SIDE_W = 170;
	private float spin = 160;
	private boolean dragging;
	private int serverCount;
	private int worldCount;

	public KromviaTitleScreen() {
		super(Component.literal("Kromvia"));
	}

	private int panelX() {
		return 0;
	}

	private int menuH() {
		return (Social.available() ? 6 : 5) * 26 + 30;
	}

	private boolean compact() {
		return height < BAR + 84 + menuH() + 30;
	}

	private int menuTop() {
		if (compact()) return BAR + 12;
		return Math.min(Math.max(BAR + 90, height / 2 - 30), height - menuH() - 18);
	}

	@Override
	protected void init() {
		Rooms.hostNext(false);
		int x = panelX() + (PANEL_W - BUTTON_W) / 2;
		int y = menuTop();
		addRenderableWidget(new KButton(x, y, BUTTON_W, 22, Component.literal("Singleplayer"),
				b -> Mc.setScreen(new SelectWorldScreen(this))).appear(0));
		addRenderableWidget(new KButton(x, y + 26, BUTTON_W, 22, Component.literal("Multiplayer"),
				b -> Mc.setScreen(new JoinMultiplayerScreen(this))).appear(40));
		addRenderableWidget(new KButton(x, y + 52, BUTTON_W, 22, Component.literal(Rooms.relay() ? "Host World" : "Host World (LAN)"), b -> {
			Rooms.hostNext(true);
			Mc.setScreen(new SelectWorldScreen(this));
		}).appear(80));
		int row = 78;
		if (Social.available()) {
			addRenderableWidget(new KButton(x, y + row, BUTTON_W, 22, Component.literal("Social"),
					b -> Mc.setScreen(new SocialScreen(this))).appear(120));
			row += 26;
		}
		addRenderableWidget(new KButton(x, y + row, BUTTON_W, 22, Component.literal("Wardrobe"),
				b -> Mc.setScreen(new CosmeticsScreen(this))).appear(160));
		addRenderableWidget(new KButton(x, y + row + 26, BUTTON_W, 22, Component.literal("Options"),
				b -> Mc.setScreen(new OptionsScreen(this, minecraft.options))).appear(200));
		addRenderableWidget(new KButton(x, y + row + 60, BUTTON_W, 22, Component.literal("Quit"), b -> minecraft.stop()).appear(240));

		ServerList list = new ServerList(minecraft);
		list.load();
		serverCount = Math.min(3, list.size());
		int sx = width - SIDE_W - 10;
		for (int i = 0; i < serverCount; i++) {
			ServerData data = list.get(i);
			addRenderableWidget(new KButton(sx, BAR + 26 + i * 24, SIDE_W, 20, Component.literal("▶ " + data.name),
					b -> ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(data.ip), data, false, null)));
		}

		List<String> worlds = recentWorlds(new File(minecraft.gameDirectory, "saves"), 3);
		worldCount = worlds.size();
		for (int i = 0; i < worldCount; i++) {
			String id = worlds.get(i);
			addRenderableWidget(new KButton(sx, worldsTop() + 14 + i * 24, SIDE_W, 20, Component.literal("▶ " + id), b -> openWorld(id)));
		}
	}

	private int worldsTop() {
		return BAR + 12 + (serverCount > 0 ? 14 + serverCount * 24 + 10 : 0);
	}

	private static List<String> recentWorlds(File saves, int max) {
		File[] dirs = saves.listFiles(f -> new File(f, "level.dat").isFile());
		List<String> out = new ArrayList<>();
		if (dirs == null) return out;
		Arrays.sort(dirs, (a, b) -> Long.compare(new File(b, "level.dat").lastModified(), new File(a, "level.dat").lastModified()));
		for (int i = 0; i < Math.min(max, dirs.length); i++) out.add(dirs[i].getName());
		return out;
	}

	private void openWorld(String id) {
		Runnable back = () -> Mc.setScreen(this);
		minecraft.createWorldOpenFlows().openWorld(id, back);
	}

	@Override
	protected void drawBackground(Gfx g, int mouseX, int mouseY, float delta) {
		drawPanorama(g, delta);
		g.fillGradient(0, height * 2 / 3, width, height, 0x000B0D11, 0xB00B0D11);
		g.fill(0, 0, width, BAR, 0xC80B0D11);
		g.fill(0, BAR, width, BAR + 1, UiStyle.LINE);
		g.fill(0, BAR + 1, PANEL_W, height, 0xE00B0D11);
		g.fill(PANEL_W, BAR + 1, PANEL_W + 1, height, UiStyle.LINE);
	}

	@Override
	protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
		super.draw(g, mouseX, mouseY, delta);

		UiStyle.logo(g, 8, 5, 16);
		g.drawString(font, "KROMVIA", 28, 9, UiStyle.TEXT, false);
		String name = minecraft.getUser().getName();
		int nw = font.width(name);
		if (Accounts.available() && chipHit(mouseX, mouseY, nw)) g.fill(width - nw - 36, 2, width - 2, BAR - 2, 0x338B5CF6);
		g.face(PlayerPreview.skin(), width - nw - 30, 5, 16);
		g.drawString(font, name, width - nw - 8, 9, UiStyle.TEXT, false);
		String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
		g.drawCenteredString(font, time, width / 2, 9, UiStyle.MUTED);

		if (serverCount > 0) g.drawString(font, "QUICK JOIN", width - SIDE_W - 10, BAR + 12, UiStyle.FRONT, false);
		if (worldCount > 0) g.drawString(font, "RECENT WORLDS", width - SIDE_W - 10, worldsTop(), UiStyle.FRONT, false);

		if (!compact()) {
			int cx = panelX() + PANEL_W / 2;
			int top = menuTop() - 84 + Math.round(net.minecraft.util.Mth.sin(System.currentTimeMillis() / 700F) * 2);
			UiStyle.logo(g, cx - 28, top, 56);
			g.pose().pushMatrix();
			g.pose().translate(cx, top + 60);
			g.pose().scale(2, 2);
			g.drawCenteredString(font, "KROMVIA", 0, 0, UiStyle.TEXT);
			g.pose().popMatrix();
		}

		if (!dragging) spin += delta * 0.6F;
		int ph = Math.min(height / 2, 200);
		int pw = ph / 2;
		int px = PANEL_W + (width - PANEL_W) / 2;
		int py2 = height - 34;
		PlayerPreview.render(g, px - pw, py2 - ph, px + pw, py2, spin, (System.currentTimeMillis() % 1_000_000L) / 50F);
		g.fill(px - pw / 2, py2 + 2, px + pw / 2, py2 + 3, 0x668B5CF6);

		g.drawString(font, "Kromvia 0.1 · Minecraft " + SharedConstants.getCurrentVersion().name(), 8, height - 12, UiStyle.MUTED, false);
		String played = PlayTime.line();
		if (played != null) g.drawString(font, "Played " + played, 8, height - 24, UiStyle.MUTED, false);
		String legal = "Not an official Minecraft product";
		g.drawString(font, legal, width - font.width(legal) - 8, height - 12, 0xFF5B616B, false);
	}

	private boolean chipHit(double x, double y, int nameWidth) {
		return x >= width - nameWidth - 36 && x < width - 2 && y >= 2 && y < BAR - 2;
	}

	@Override
	protected void mouseDown(double x, double y) {
		if (Accounts.available() && chipHit(x, y, font.width(minecraft.getUser().getName()))) {
			Mc.setScreen(new AccountsScreen(this));
			return;
		}
		dragging = x > PANEL_W;
	}

	@Override
	protected boolean mouseDrag(double dx, double dy) {
		if (dragging) spin -= (float) dx * 1.5F;
		return dragging;
	}

	@Override
	protected void mouseUp() {
		dragging = false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
