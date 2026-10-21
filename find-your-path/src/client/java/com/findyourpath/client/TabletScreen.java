package com.findyourpath.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One screen layout used for every view: a centered stone tablet with a header,
 * an item icon + heading, a few paragraphs, an optional recipe, and buttons.
 */
public class TabletScreen extends Screen {
	public record Para(String text, int color) {}
	public record Btn(String label, Runnable action) {}

	private String header = "Find Your Path";
	private int headerColor = Ui.GOLD;
	private Item icon;
	private String heading;
	private int headingColor = Ui.WHITE;
	private final List<Para> paras = new ArrayList<>();
	private Item[] recipe;
	private Item recipeResult;
	private int pipCurrent, pipTotal;
	private final List<Btn> buttons = new ArrayList<>();
	private int buttonColumns = 1;
	private Runnable onClose;

	// layout, computed in init()
	private int tx, ty, tw, th;
	private final List<List<FormattedCharSequence>> wrapped = new ArrayList<>();
	private List<FormattedCharSequence> headingLines = List.of();

	public TabletScreen() {
		super(Component.literal("Find Your Path"));
	}

	// ---- builder-style setup
	public TabletScreen header(String text, int color) { this.header = text; this.headerColor = color; return this; }
	public TabletScreen heading(Item icon, String text, int color) { this.icon = icon; this.heading = text; this.headingColor = color; return this; }
	public TabletScreen para(String text) { return para(text, Ui.SOFT); }
	public TabletScreen para(String text, int color) { paras.add(new Para(text, color)); return this; }
	public TabletScreen recipe(Item[] grid, Item result) { this.recipe = grid; this.recipeResult = result; return this; }
	public TabletScreen progress(int current, int total) { this.pipCurrent = current; this.pipTotal = total; return this; }
	public TabletScreen button(String label, Runnable action) { buttons.add(new Btn(label, action)); return this; }
	public TabletScreen columns(int n) { this.buttonColumns = n; return this; }
	public TabletScreen onClose(Runnable r) { this.onClose = r; return this; }

	private static final int PAD = 14, LINE = 11, BTN_H = 20, BTN_GAP = 4;

	@Override
	protected void init() {
		tw = Math.min(320, this.width - 24);
		int textW = tw - PAD * 2;

		wrapped.clear();
		for (Para p : paras) wrapped.add(this.font.split(Component.literal(p.text()), textW));
		int headingTextW = textW - (icon != null ? 24 : 0);
		headingLines = heading == null ? List.of() : this.font.split(Component.literal(heading), headingTextW);

		int h = PAD;
		h += 12;                                                      // header row
		if (pipTotal > 0) h += 10;
		if (heading != null) h += Math.max(20, headingLines.size() * LINE) + 8;
		for (List<FormattedCharSequence> lines : wrapped) h += lines.size() * LINE + 6;
		if (recipe != null) h += Ui.RECIPE_H + 14;
		int rows = (buttons.size() + buttonColumns - 1) / buttonColumns;
		h += 4 + rows * (BTN_H + BTN_GAP);
		h += PAD - BTN_GAP;

		th = Math.min(h, this.height - 16);
		tx = (this.width - tw) / 2;
		ty = Math.max(8, (this.height - th) / 2);

		int bw = (textW - (buttonColumns - 1) * BTN_GAP) / buttonColumns;
		int by = ty + th - PAD + BTN_GAP - rows * (BTN_H + BTN_GAP);
		for (int i = 0; i < buttons.size(); i++) {
			Btn b = buttons.get(i);
			int col = i % buttonColumns, row = i / buttonColumns;
			this.addRenderableWidget(Button.builder(Component.literal(b.label()), btn -> b.action().run())
					.bounds(tx + PAD + col * (bw + BTN_GAP), by + row * (BTN_H + BTN_GAP), bw, BTN_H).build());
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		Ui.tablet(g, tx, ty, tw, th);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);

		int x = tx + PAD, y = ty + PAD;
		g.text(this.font, header, x, y, headerColor, true);
		y += 12;
		if (pipTotal > 0) {
			Ui.pips(g, x, y, pipCurrent, pipTotal);
			y += 10;
		}
		if (heading != null) {
			int blockH = Math.max(20, headingLines.size() * LINE);
			int tx2 = x;
			if (icon != null) {
				g.item(new ItemStack(icon), x, y + (blockH - 16) / 2);
				tx2 += 24;
			}
			int ly = y + (blockH - headingLines.size() * LINE) / 2 + 1;
			for (FormattedCharSequence line : headingLines) {
				g.text(this.font, line, tx2, ly, headingColor, true);
				ly += LINE;
			}
			y += blockH + 8;
		}
		for (int i = 0; i < wrapped.size(); i++) {
			int color = paras.get(i).color();
			for (FormattedCharSequence line : wrapped.get(i)) {
				g.text(this.font, line, x, y, color, true);
				y += LINE;
			}
			y += 6;
		}
		if (recipe != null) {
			int rx = tx + (tw - Ui.RECIPE_W) / 2;
			Ui.inset(g, rx - 6, y - 4, Ui.RECIPE_W + 12, Ui.RECIPE_H + 8);
			Ui.recipe(g, this.font, rx, y, recipe, recipeResult);
		}
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(null);
		if (onClose != null) onClose.run();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
