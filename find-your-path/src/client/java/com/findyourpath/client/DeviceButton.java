package com.findyourpath.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/**
 * A clean, angled button for the alien-device screens.
 * With an icon it becomes a tile: big icon on top, label underneath.
 */
public class DeviceButton extends AbstractWidget {
	private final Runnable action;
	private final int accent;
	private final Item icon;

	public DeviceButton(int x, int y, int w, int h, String label, int accent, Item icon, Runnable action) {
		super(x, y, w, h, Component.literal(label));
		this.action = action;
		this.accent = accent;
		this.icon = icon;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		boolean hot = this.isHoveredOrFocused();
		int x = getX(), y = getY(), w = getWidth(), h = getHeight();
		var font = Minecraft.getInstance().font;
		if (hot) Ui.chamfer(g, x - 2, y - 2, w + 4, h + 4, 6, Ui.withAlpha(accent, 0x40));
		Ui.chamfer(g, x, y, w, h, 4, hot ? accent : Ui.withAlpha(accent, 0x90));
		Ui.chamfer(g, x + 1, y + 1, w - 2, h - 2, 3, hot ? Ui.mix(0xFF0A0D1E, accent, 0.28f) : 0xFF101530);
		String label = getMessage().getString();
		if (icon != null) {
			Ui.bigItem(g, icon, x + w / 2 - 16, y + 7, 2f);
			Ui.textCentered(g, font, label, x + w / 2, y + h - 13, hot ? Ui.WHITE : accent);
		} else {
			Ui.textCentered(g, font, label, x + w / 2, y + (h - 8) / 2, hot ? Ui.WHITE : Ui.SOFT);
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.playDownSound(Minecraft.getInstance().getSoundManager());
		action.run();
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
