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

import com.findyourpath.client.Quests.Pic;

/**
 * One layout used for every view: a glowing alien-device panel that slides up into the middle of the screen.
 * It can hold a header, an icon + heading, paragraphs, a recipe or a tip picture, icon tiles, and buttons.
 */
public class TabletScreen extends Screen {
	public record Para(String text, int color) {}
	public record Btn(String label, Runnable action) {}
	public record Tile(String label, Item icon, int color, Runnable action) {}

	private String header = "FIND YOUR PATH";
	private int accent = Ui.CYAN;
	private Item icon;
	private String heading;
	private int headingColor = Ui.WHITE;
	private final List<Para> paras = new ArrayList<>();
	private Item[] recipe;
	private Item recipeResult;
	private List<Pic> picture;
	private int pipCurrent, pipTotal;
	private final List<Tile> tiles = new ArrayList<>();
	private final List<Btn> buttons = new ArrayList<>();
	private int buttonColumns = 1;
	private Runnable onClose;

	// layout, computed in init()
	private int tx, ty, tw, th;
	private final List<List<FormattedCharSequence>> wrapped = new ArrayList<>();
	private List<FormattedCharSequence> headingLines = List.of();
	private final List<DeviceButton> widgets = new ArrayList<>();
	private final List<Integer> widgetBaseY = new ArrayList<>();

	private static final long RISE_MS = 240;
	private long openedAt = -1;

	public TabletScreen() {
		super(Component.literal("Find Your Path"));
	}

	// ---- builder-style setup
	public TabletScreen header(String text, int color) { this.header = text.toUpperCase(); this.accent = color; return this; }
	public TabletScreen accent(int color) { this.accent = color; return this; }
	public TabletScreen heading(Item icon, String text, int color) { this.icon = icon; this.heading = text; this.headingColor = color; return this; }
	public TabletScreen para(String text) { return para(text, Ui.SOFT); }
	public TabletScreen para(String text, int color) { paras.add(new Para(text, color)); return this; }
	public TabletScreen recipe(Item[] grid, Item result) { this.recipe = grid; this.recipeResult = result; return this; }
	public TabletScreen picture(List<Pic> pic) { this.picture = pic; return this; }
	public TabletScreen progress(int current, int total) { this.pipCurrent = current; this.pipTotal = total; return this; }
	public TabletScreen tile(String label, Item icon, int color, Runnable action) { tiles.add(new Tile(label, icon, color, action)); return this; }
	public TabletScreen button(String label, Runnable action) { buttons.add(new Btn(label, action)); return this; }
	public TabletScreen columns(int n) { this.buttonColumns = n; return this; }
	public TabletScreen onClose(Runnable r) { this.onClose = r; return this; }

	private static final int PAD = 16, LINE = 11, BTN_H = 20, BTN_GAP = 5, TILE_W = 88, TILE_H = 56, TILE_GAP = 8, PIC_H = 58;

	@Override
	protected void init() {
		if (openedAt < 0) {
			openedAt = System.currentTimeMillis();
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_ACTIVATE, 1.8f, 0.25f));
		}
		int tileCols = Math.min(3, Math.max(1, tiles.size()));
		int tilesW = tileCols * TILE_W + (tileCols - 1) * TILE_GAP;
		tw = Math.min(Math.max(320, tilesW + PAD * 2), this.width - 24);
		int textW = tw - PAD * 2;

		wrapped.clear();
		for (Para p : paras) wrapped.add(Ui.wrap(this.font, p.text(), textW));
		int headingTextW = textW - (icon != null ? 38 : 0);
		headingLines = heading == null ? List.of() : Ui.wrap(this.font, heading, headingTextW);

		int h = 30;                                                   // header band
		if (pipTotal > 0) h += 10;
		if (heading != null) h += Math.max(34, headingLines.size() * LINE) + 8;
		for (List<FormattedCharSequence> lines : wrapped) h += lines.size() * LINE + 6;
		if (recipe != null) h += Ui.recipeH(recipe) + 18;
		if (picture != null) h += PIC_H + 10;
		int tileRows = (tiles.size() + tileCols - 1) / tileCols;
		if (!tiles.isEmpty()) h += tileRows * (TILE_H + TILE_GAP) + 2;
		int rows = (buttons.size() + buttonColumns - 1) / buttonColumns;
		h += 2 + rows * (BTN_H + BTN_GAP);
		h += PAD - BTN_GAP;

		th = Math.min(h, this.height - 16);
		tx = (this.width - tw) / 2;
		ty = Math.max(8, (this.height - th) / 2);

		widgets.clear();
		widgetBaseY.clear();
		int by = ty + th - PAD + BTN_GAP - rows * (BTN_H + BTN_GAP);
		if (!tiles.isEmpty()) {
			int tyTiles = by - 2 - tileRows * (TILE_H + TILE_GAP);
			for (int i = 0; i < tiles.size(); i++) {
				Tile t = tiles.get(i);
				int row = i / tileCols, col = i % tileCols;
				int inRow = Math.min(tileCols, tiles.size() - row * tileCols);
				int rowW = inRow * TILE_W + (inRow - 1) * TILE_GAP;
				int x = tx + (tw - rowW) / 2 + col * (TILE_W + TILE_GAP);
				int y = tyTiles + row * (TILE_H + TILE_GAP);
				add(new DeviceButton(x, y, TILE_W, TILE_H, t.label(), t.color(), t.icon(), t.action()), y);
			}
		}
		int bw = (textW - (buttonColumns - 1) * BTN_GAP) / buttonColumns;
		for (int i = 0; i < buttons.size(); i++) {
			Btn b = buttons.get(i);
			int col = i % buttonColumns, row = i / buttonColumns;
			int y = by + row * (BTN_H + BTN_GAP);
			add(new DeviceButton(tx + PAD + col * (bw + BTN_GAP), y, bw, BTN_H, b.label(), accent, null, b.action()), y);
		}
	}

	private void add(DeviceButton b, int baseY) {
		widgets.add(b);
		widgetBaseY.add(baseY);
		this.addRenderableWidget(b);
	}

	/** How far below its resting place the panel is right now (eases from 20px to 0). */
	private int rise() {
		float t = Math.min(1f, (System.currentTimeMillis() - openedAt) / (float) RISE_MS);
		float eased = 1 - (1 - t) * (1 - t) * (1 - t);
		return Math.round((1 - eased) * 20);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		int off = rise();
		for (int i = 0; i < widgets.size(); i++) widgets.get(i).setY(widgetBaseY.get(i) + off);
		Ui.panel(g, tx, ty + off, tw, th, accent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);

		int off = rise();
		int x = tx + PAD, y = ty + off;
		Ui.text(g, this.font, header, x, y + 8, accent);
		y += 30;
		if (pipTotal > 0) {
			Ui.progressBar(g, x, y, tw - PAD * 2, pipCurrent, pipTotal, accent);
			y += 10;
		}
		if (heading != null) {
			int blockH = Math.max(34, headingLines.size() * LINE);
			int tx2 = x;
			if (icon != null) {
				Ui.display(g, x, y + (blockH - 34) / 2, 34, 34, accent);
				Ui.bigItem(g, icon, x + 1, y + (blockH - 34) / 2 + 1, 2f);
				tx2 += 42;
			}
			int ly = y + (blockH - headingLines.size() * LINE) / 2 + 1;
			for (FormattedCharSequence line : headingLines) {
				Ui.text(g, this.font, line, tx2, ly, headingColor);
				ly += LINE;
			}
			y += blockH + 8;
		}
		for (int i = 0; i < wrapped.size(); i++) {
			int color = paras.get(i).color();
			for (FormattedCharSequence line : wrapped.get(i)) {
				Ui.text(g, this.font, line, x, y, color);
				y += LINE;
			}
			y += 6;
		}
		if (recipe != null) {
			int rw = Ui.recipeW(recipe), rh = Ui.recipeH(recipe);
			int rx = tx + (tw - rw) / 2;
			Ui.display(g, rx - 8, y, rw + 16, rh + 12, accent);
			Ui.recipe(g, this.font, rx, y + 6, recipe, recipeResult, accent);
			y += rh + 18;
		}
		if (picture != null) {
			Ui.display(g, x, y, tw - PAD * 2, PIC_H, accent);
			drawPicture(g, tx + tw / 2, y + PIC_H / 2, picture);
		}
	}

	/** Lays out a tip picture: big items and glyphs in a row, centered. */
	private void drawPicture(GuiGraphicsExtractor g, int cx, int cy, List<Pic> pic) {
		int cell = 44, total = pic.size() * cell;
		int x = cx - total / 2;
		for (Pic p : pic) {
			int mid = x + cell / 2;
			if (p.item() != null) Ui.bigItem(g, p.item(), mid - 16, cy - 16, 2f);
			else Ui.bigText(g, this.font, p.glyph(), mid, cy, 2.5f, p.color());
			x += cell;
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
