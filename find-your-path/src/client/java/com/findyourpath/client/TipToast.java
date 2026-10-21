package com.findyourpath.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.world.item.ItemStack;

/** An advancement-style pop-up in the corner, with our own header text. */
public class TipToast implements Toast {
	private static final long SHOW_MS = 7000;
	private final Quests.Tip tip;
	private Toast.Visibility visibility = Toast.Visibility.SHOW;

	public TipToast(Quests.Tip tip) {
		this.tip = tip;
	}

	@Override
	public int width() {
		Font f = net.minecraft.client.Minecraft.getInstance().font;
		return Math.max(160, 38 + Math.max(f.width("Tip: " + tip.title()), f.width(tip.text())));
	}

	@Override
	public Toast.Visibility getWantedVisibility() {
		return visibility;
	}

	@Override
	public void update(ToastManager manager, long fullyVisibleForMs) {
		if (fullyVisibleForMs >= SHOW_MS) visibility = Toast.Visibility.HIDE;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, Font font, long fullyVisibleForMs) {
		int w = width(), h = height();
		g.fill(0, 0, w, h, 0xFF141414);
		g.fill(1, 1, w - 1, h - 1, 0xFF3A3A3A);
		g.fill(2, 2, w - 2, h - 2, 0xFF5E5E5E);
		g.item(new ItemStack(tip.icon()), 8, 8);
		g.text(font, "Tip: " + tip.title(), 30, 7, Ui.GOLD, true);
		g.text(font, tip.text(), 30, 18, Ui.WHITE, true);
	}
}
