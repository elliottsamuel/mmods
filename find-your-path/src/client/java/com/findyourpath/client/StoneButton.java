package com.findyourpath.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** A button chiselled into the tablet, with an engraved label. */
public class StoneButton extends Button {
	public StoneButton(int x, int y, int w, int h, Component label, OnPress onPress) {
		super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		boolean hot = this.isHoveredOrFocused();
		Ui.sprite(g, hot ? Ui.BUTTON_HOVER : Ui.BUTTON, getX(), getY(), getWidth(), getHeight());
		var font = Minecraft.getInstance().font;
		String label = getMessage().getString();
		Ui.carvedCentered(g, font, label, getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, hot ? 0xFF1A1813 : Ui.INK_SOFT);
	}
}
