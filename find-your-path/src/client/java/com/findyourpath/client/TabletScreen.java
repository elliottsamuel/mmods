package com.findyourpath.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One layout for every view: a carved stone tablet that rises into the middle of the screen,
 * with engraved header, an item icon + heading, paragraphs, an optional recipe, and chiselled buttons.
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
	private final List<StoneButton> widgets = new ArrayList<>();
	private final List<Integer> widgetBaseY = new ArrayList<>();

	// rise-in animation
	private static final long RISE_MS = 260;
	private long openedAt = -1;

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

	private static final int PAD = 20, LINE = 11, BTN_H = 20, BTN_GAP = 5;

	@Override
	protected void init() {
		if (openedAt < 0) {
			openedAt = System.currentTimeMillis();
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.GRINDSTONE_USE, 0.6f, 0.35f));
		}
		tw = Math.min(330, this.width - 24);
		int textW = tw - PAD * 2;

		wrapped.clear();
		for (Para p : paras) wrapped.add(Ui.wrap(this.font, p.text(), textW));
		int headingTextW = textW - (icon != null ? 24 : 0);
		headingLines = heading == null ? List.of() : Ui.wrap(this.font, heading, headingTextW);

		int h = PAD;
		h += 13;                                                      // header row
		if (pipTotal > 0) h += 12;
		if (heading != null) h += Math.max(20, headingLines.size() * LINE) + 8;
		for (List<FormattedCharSequence> lines : wrapped) h += lines.size() * LINE + 6;
		if (recipe != null) h += Ui.recipeH(recipe) + 16;
		int rows = (buttons.size() + buttonColumns - 1) / buttonColumns;
		h += 4 + rows * (BTN_H + BTN_GAP);
		h += PAD - BTN_GAP;

		th = Math.min(h, this.height - 16);
		tx = (this.width - tw) / 2;
		ty = Math.max(8, (this.height - th) / 2);

		widgets.clear();
		widgetBaseY.clear();
		int bw = (textW - (buttonColumns - 1) * BTN_GAP) / buttonColumns;
		int by = ty + th - PAD + BTN_GAP - rows * (BTN_H + BTN_GAP);
		for (int i = 0; i < buttons.size(); i++) {
			Btn b = buttons.get(i);
			int col = i % buttonColumns, row = i / buttonColumns;
			int y = by + row * (BTN_H + BTN_GAP);
			StoneButton w = new StoneButton(tx + PAD + col * (bw + BTN_GAP), y, bw, BTN_H, Component.literal(b.label()), btn -> b.action().run());
			widgets.add(w);
			widgetBaseY.add(y);
			this.addRenderableWidget(w);
		}
	}

	/** How far below its resting place the tablet is right now (eases from 24px to 0). */
	private int rise() {
		float t = Math.min(1f, (System.currentTimeMillis() - openedAt) / (float) RISE_MS);
		float eased = 1 - (1 - t) * (1 - t) * (1 - t);
		return Math.round((1 - eased) * 24);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		int off = rise();
		for (int i = 0; i < widgets.size(); i++) widgets.get(i).setY(widgetBaseY.get(i) + off);
		Ui.tablet(g, tx, ty + off, tw, th);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);

		int off = rise();
		int x = tx + PAD, y = ty + PAD + off;
		Ui.carved(g, this.font, header, x, y, Ui.ink(headerColor));
		y += 13;
		if (pipTotal > 0) {
			Ui.notches(g, x, y, pipCurrent, pipTotal);
			y += 12;
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
				Ui.carved(g, this.font, line, tx2, ly, Ui.ink(headingColor));
				ly += LINE;
			}
			y += blockH + 8;
		}
		for (int i = 0; i < wrapped.size(); i++) {
			int color = Ui.ink(paras.get(i).color());
			for (FormattedCharSequence line : wrapped.get(i)) {
				Ui.carved(g, this.font, line, x, y, color);
				y += LINE;
			}
			y += 6;
		}
		if (recipe != null) {
			int rx = tx + (tw - Ui.recipeW(recipe)) / 2;
			Ui.recipe(g, this.font, rx, y + 2, recipe, recipeResult);
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
