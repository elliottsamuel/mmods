package com.findyourpath.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A short corner pop-up for progress, like "Got a log! 1 of 3". */
public class ProgressToast implements Toast {
	private static final long SHOW_MS = 2600;
	private final Item icon;
	private final String title, text;
	private final int accent;
	private Toast.Visibility visibility = Toast.Visibility.SHOW;

	public ProgressToast(Item icon, String title, String text, int accent) {
		this.icon = icon;
		this.title = title;
		this.text = text;
		this.accent = accent;
	}

	@Override public int width() { return 180; }
	@Override public int height() { return 32; }
	@Override public Toast.Visibility getWantedVisibility() { return visibility; }

	@Override
	public void update(ToastManager manager, long fullyVisibleForMs) {
		if (fullyVisibleForMs >= SHOW_MS) visibility = Toast.Visibility.HIDE;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, Font font, long fullyVisibleForMs) {
		Ui.smallPanel(g, 1, 1, 178, 30, accent);
		g.item(new ItemStack(icon), 9, 8);
		Ui.text(g, font, title, 32, 7, accent);
		Ui.text(g, font, text, 32, 18, Ui.SOFT);
	}
}
