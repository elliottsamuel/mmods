package com.findyourpath.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.world.item.ItemStack;

/** A tip on a small stone plaque that slides in from the corner. */
public class TipToast implements Toast {
	private static final long SHOW_MS = 7000;
	private final Quests.Tip tip;
	private Toast.Visibility visibility = Toast.Visibility.SHOW;

	public TipToast(Quests.Tip tip) {
		this.tip = tip;
	}

	@Override
	public int width() {
		return 230;
	}

	@Override
	public int height() {
		return 32;
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
		Ui.sprite(g, Ui.PLAQUE, 0, 0, 230, 32);
		g.item(new ItemStack(tip.icon()), 10, 8);
		Ui.carved(g, font, "Tip: " + tip.title(), 32, 7, 0xFF6B4A12);
		Ui.carved(g, font, tip.text(), 32, 18, Ui.INK_SOFT);
	}
}
