package com.kromvia.client.ui;

import com.kromvia.client.Social;
import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.KScreen;
import com.kromvia.client.compat.Mc;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

public class SocialScreen extends KScreen {
	private static final int TOP = 40;
	private static final int ROW = 22;
	private static final int LEFT_W = 160;
	private static final int PARTY_MAX = 8;
	private final Screen parent;
	private EditBox input;
	private String draft = "";
	private int builtW, builtH;
	private String built = "";

	public SocialScreen(Screen parent) {
		super(Component.literal("Social"));
		this.parent = parent;
		Social.load();
	}

	private int panelW() {
		return Math.min(width - 20, 480);
	}

	private int left() {
		return (width - panelW()) / 2;
	}

	private int right() {
		return left() + LEFT_W + 10;
	}

	private int rightW() {
		return panelW() - LEFT_W - 10;
	}

	private int bottom() {
		return height - 30;
	}

	private int chatTop() {
		return TOP + (Social.inParty() ? 50 : 30) + Social.invites().size() * ROW;
	}

	private static String layoutKey() {
		StringBuilder k = new StringBuilder();
		k.append(Social.received()).append(Social.signedOut()).append(Social.inParty()).append(Social.leads()).append(Social.room()).append(Social.chatWith());
		for (Social.Friend f : Social.friends()) k.append('|').append(f.id).append(f.online).append(f.unread);
		for (String m : Social.members()) k.append('#').append(m);
		for (Social.Invite i : Social.invites()) k.append('!').append(i.party);
		return k.toString();
	}

	@Override
	protected void init() {
		built = layoutKey();
		int x = left();
		int rows = Math.max(1, (bottom() - TOP - 14) / ROW);
		List<Social.Friend> friends = sorted();
		boolean canInvite = !Social.inParty() || Social.leads() && Social.members().size() < PARTY_MAX;
		for (int i = 0; i < Math.min(rows, friends.size()); i++) {
			Social.Friend f = friends.get(i);
			String dot = f.online ? "§a● §r" : "§8● §7";
			String label = dot + f.name + (f.unread > 0 ? "  §d" + f.unread : "");
			int y = TOP + 14 + i * ROW;
			boolean invite = canInvite && f.online && !Social.members().contains(f.name);
			addRenderableWidget(new KButton(x, y, invite ? LEFT_W - 24 : LEFT_W, ROW - 2, Component.literal(label), b -> Social.open(f.id),
					() -> f.id.equals(Social.chatWith())));
			if (invite) addRenderableWidget(new KButton(x + LEFT_W - 22, y, 22, ROW - 2, Component.literal("+"), b -> Social.invite(f.id)).flat());
		}

		int rx = right();
		int rw = rightW();
		if (Social.inParty()) {
			String room = Social.room();
			int bw = room != null && !Social.leads() ? (rw - 4) / 2 : rw;
			addRenderableWidget(new KButton(rx, TOP + 26, bw, 20, Component.literal("Leave party"), b -> Social.leave()));
			if (room != null && !Social.leads()) addRenderableWidget(new KButton(rx + bw + 4, TOP + 26, bw, 20, Component.literal("Join room"), b -> join(room)));
		}
		int iy = TOP + (Social.inParty() ? 50 : 30);
		for (Social.Invite inv : Social.invites()) {
			addRenderableWidget(new KButton(rx + rw - 84, iy, 56, ROW - 2, Component.literal("Join"), b -> Social.join(inv.party)));
			addRenderableWidget(new KButton(rx + rw - 24, iy, 24, ROW - 2, Component.literal("✕"), b -> Social.decline(inv.party)).flat());
			iy += ROW;
		}

		if (input == null || builtW != width || builtH != height) {
			input = new EditBox(font, rx, bottom() - 20, rw - 54, 20, Component.literal("Message"));
			input.setMaxLength(500);
			input.setValue(draft);
			input.setResponder(v -> draft = v);
			builtW = width;
			builtH = height;
		}
		if (Social.chatWith() != null) {
			addRenderableWidget(input);
			addRenderableWidget(new KButton(rx + rw - 50, bottom() - 20, 50, 20, Component.literal("Send"), b -> send()));
		}
		addRenderableWidget(new KButton(x, height - 24, 80, 20, Component.literal("Back"), b -> onClose()));
	}

	private static List<Social.Friend> sorted() {
		List<Social.Friend> list = new ArrayList<>(Social.friends());
		list.sort((a, b) -> a.online != b.online ? (a.online ? -1 : 1) : a.name.compareToIgnoreCase(b.name));
		return list;
	}

	private void send() {
		String to = Social.chatWith();
		String text = draft.trim();
		if (to == null || text.isEmpty()) return;
		Social.send(to, text);
		draft = "";
		input.setValue("");
	}

	private void join(String room) {
		ServerData data = new ServerData("Kromvia room", room, ServerData.Type.OTHER);
		ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(data.ip), data, false, null);
	}

	@Override
	public void tick() {
		super.tick();
		if (!built.equals(layoutKey())) {
			clearWidgets();
			init();
		}
	}

	@Override
	protected boolean keyDown(int key) {
		if ((key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) && input != null && input.isFocused()) {
			send();
			return true;
		}
		return false;
	}

	@Override
	protected void drawBackground(Gfx g, int mouseX, int mouseY, float delta) {
		UiStyle.dim(g, width, height, 1);
	}

	@Override
	protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
		super.draw(g, mouseX, mouseY, delta);
		int x = left();
		int rx = right();
		int rw = rightW();
		g.drawString(font, "SOCIAL", x, 14, UiStyle.TEXT, false);
		String status = status();
		if (status != null) g.drawString(font, status, x + 60, 14, UiStyle.MUTED, false);

		int online = 0;
		for (Social.Friend f : Social.friends()) if (f.online) online++;
		g.drawString(font, "FRIENDS  " + online + "/" + Social.friends().size(), x, TOP, UiStyle.FRONT, false);
		if (Social.received() && Social.friends().isEmpty() && !Social.signedOut()) {
			g.drawString(font, "Add friends in the launcher.", x, TOP + 16, UiStyle.MUTED, false);
		}

		if (Social.inParty()) {
			g.drawString(font, "PARTY  " + Social.members().size() + "/" + PARTY_MAX + (Social.leads() ? "  · you lead" : ""), rx, TOP, UiStyle.FRONT, false);
			g.drawString(font, fit(String.join(", ", Social.members()), rw), rx, TOP + 12, UiStyle.TEXT, false);
		} else {
			g.drawString(font, "PARTY", rx, TOP, UiStyle.FRONT, false);
			g.drawString(font, "Not in a party. + invites an online friend.", rx, TOP + 12, UiStyle.MUTED, false);
		}
		int iy = TOP + (Social.inParty() ? 50 : 30);
		for (Social.Invite inv : Social.invites()) {
			g.drawString(font, fit((inv.from == null ? "Someone" : inv.from) + " invited you", rw - 90), rx, iy + 6, UiStyle.TEXT, false);
			iy += ROW;
		}

		String with = Social.chatWith();
		int top = chatTop();
		if (with == null) {
			g.drawString(font, "Pick a friend to chat.", rx, top + 4, UiStyle.MUTED, false);
			return;
		}
		String name = nameOf(with);
		g.drawString(font, "CHAT  · " + name, rx, top, UiStyle.FRONT, false);
		List<String> wrapped = new ArrayList<>();
		List<Integer> colours = new ArrayList<>();
		for (Social.Line l : Social.lines()) {
			for (String part : wrap((l.mine ? "You: " : name + ": ") + l.text, rw)) {
				wrapped.add(part);
				colours.add(l.mine ? UiStyle.MUTED : UiStyle.TEXT);
			}
		}
		int lineH = font.lineHeight + 2;
		int fits = Math.max(0, (bottom() - 26 - (top + 14)) / lineH);
		int from = Math.max(0, wrapped.size() - fits);
		for (int i = from; i < wrapped.size(); i++) {
			g.drawString(font, wrapped.get(i), rx, top + 14 + (i - from) * lineH, colours.get(i), false);
		}
		if (wrapped.isEmpty()) g.drawString(font, "No messages yet.", rx, top + 14, UiStyle.MUTED, false);
	}

	private static String status() {
		if (!Social.received()) return "Waiting for the launcher…";
		if (Social.signedOut()) return "Make a Kromvia account in the launcher (Friends).";
		if (Social.error() != null) return Social.error();
		if (!Social.connected()) return "Connecting to Kromvia…";
		return null;
	}

	private static String nameOf(String id) {
		for (Social.Friend f : Social.friends()) if (f.id.equals(id)) return f.name;
		return "?";
	}

	private String fit(String s, int w) {
		if (font.width(s) <= w) return s;
		while (!s.isEmpty() && font.width(s + "…") > w) s = s.substring(0, s.length() - 1);
		return s + "…";
	}

	private List<String> wrap(String s, int w) {
		List<String> out = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : s.split(" ")) {
			String next = line.length() == 0 ? word : line + " " + word;
			if (font.width(next) <= w) {
				line.setLength(0);
				line.append(next);
				continue;
			}
			if (line.length() > 0) out.add(line.toString());
			line.setLength(0);
			while (font.width(word) > w) {
				int n = word.length();
				while (n > 1 && font.width(word.substring(0, n)) > w) n--;
				out.add(word.substring(0, n));
				word = word.substring(n);
			}
			line.append(word);
		}
		if (line.length() > 0) out.add(line.toString());
		return out;
	}

	@Override
	public void onClose() {
		Mc.setScreen(parent);
	}
}
