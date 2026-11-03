package com.kromvia.client.ui;

import com.kromvia.client.Accounts;
import com.kromvia.client.compat.Gfx;
import com.kromvia.client.compat.KScreen;
import com.kromvia.client.compat.Mc;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AccountsScreen extends KScreen {
	private static final int ROW_W = 220;
	private static final int ROW_H = 22;
	private final Screen parent;

	public AccountsScreen(Screen parent) {
		super(Component.literal("Accounts"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = (width - ROW_W) / 2;
		int y = height / 2 - Accounts.list().size() * (ROW_H + 4) / 2;
		int i = 0;
		for (Accounts.Entry e : Accounts.list()) {
			String label = e.name() + (e.id() == null ? "  (offline)" : "") + (Accounts.isActive(e) ? "  ✓" : "");
			addRenderableWidget(new KButton(x, y + i * (ROW_H + 4), ROW_W, ROW_H, Component.literal(label), b -> Accounts.switchTo(e),
					() -> Accounts.isActive(e)).appear(i * 40));
			i++;
		}
		addRenderableWidget(new KButton(x, Math.min(height - 30, y + i * (ROW_H + 4) + 10), ROW_W, ROW_H, Component.literal("Back"), b -> onClose()));
	}

	@Override
	protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
		super.draw(g, mouseX, mouseY, delta);
		g.drawCenteredString(font, "ACCOUNTS", width / 2, height / 2 - Accounts.list().size() * (ROW_H + 4) / 2 - 24, UiStyle.TEXT);
		String note = Accounts.switching() ? "Restarting the game…" : "The game restarts on the account you pick.";
		g.drawCenteredString(font, note, width / 2, height - 14, UiStyle.MUTED);
	}

	@Override
	public void onClose() {
		Mc.setScreen(parent);
	}
}
