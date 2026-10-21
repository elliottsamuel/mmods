package com.findyourpath.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The "alien device" look: dark glass panels with angled corners, a glowing accent frame,
 * faint scanlines, corner nodes, and a light that sweeps along the top edge.
 * Everything is drawn in code, so edges stay crisp and any accent color can be used.
 */
public final class Ui {
	private Ui() {}

	// text colors: bright on dark glass, easy to read
	public static final int WHITE = 0xFFF4F7FF;
	public static final int SOFT = 0xFFCDD6EE;
	public static final int MUTED = 0xFF8F9BBD;
	public static final int GOLD = 0xFFFFC547;
	public static final int GREEN = 0xFF5CFFA1;

	// device palette
	public static final int CYAN = 0xFF4FE3FF;
	public static final int MAGENTA = 0xFFD06BFF;
	public static final int LIME = 0xFFB6FF5C;
	private static final int GLASS = 0xF00A0D1E;
	private static final int GLASS_HI = 0xF0121733;

	/** Text colors pass straight through now (the old stone look needed dark "ink"). */
	public static int ink(int color) { return color; }

	public static int withAlpha(int color, int alpha) { return (alpha << 24) | (color & 0xFFFFFF); }

	public static int mix(int a, int b, float t) {
		int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
		int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
		return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
	}

	/** A rectangle with its four corners cut at 45 degrees. */
	public static void chamfer(GuiGraphicsExtractor g, int x, int y, int w, int h, int cut, int color) {
		cut = Math.min(cut, Math.min(w, h) / 2);
		for (int i = 0; i < cut; i++) {
			int inset = cut - i;
			g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
			g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
		}
		g.fill(x, y + cut, x + w, y + h - cut, color);
	}

	private static float pulse(double speed) {
		return (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / speed));
	}

	/** The main device panel. */
	public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int accent) {
		int cut = 9;
		chamfer(g, x - 3, y - 3, w + 6, h + 6, cut + 2, withAlpha(accent, 0x22));   // outer glow
		chamfer(g, x - 1, y - 1, w + 2, h + 2, cut + 1, withAlpha(accent, 0x55));
		chamfer(g, x, y, w, h, cut, accent);                                         // frame
		chamfer(g, x + 1, y + 1, w - 2, h - 2, cut - 1, GLASS);                      // glass
		chamfer(g, x + 1, y + 1, w - 2, 22, cut - 1, GLASS_HI);                      // lighter header band
		g.fill(x + 10, y + 22, x + w - 10, y + 23, withAlpha(accent, 0x70));         // header divider
		for (int sy = y + 25; sy < y + h - 4; sy += 3) g.fill(x + 2, sy, x + w - 2, sy + 1, 0x07FFFFFF); // scanlines
		// light sweeping along the top edge
		long t = System.currentTimeMillis();
		int span = w + 80, sx = (int) ((t / 6) % span) - 40;
		for (int i = 0; i < 40; i++) {
			int px = x + sx + i;
			if (px > x + cut && px < x + w - cut) {
				int a = (int) (0xC0 * (1 - Math.abs(i - 20) / 20f));
				g.fill(px, y, px + 1, y + 1, withAlpha(WHITE, a));
			}
		}
		// corner nodes in a second color, softly pulsing
		int node = mix(accent, MAGENTA, 0.6f);
		int a = (int) (0x90 + 0x6F * pulse(400));
		for (int[] c : new int[][] {{x + 3, y + 3}, {x + w - 6, y + 3}, {x + 3, y + h - 6}, {x + w - 6, y + h - 6}}) {
			g.fill(c[0], c[1], c[0] + 3, c[1] + 3, withAlpha(node, a));
		}
		// little glyph marks on the header band, right side
		int gx = x + w - 34;
		g.fill(gx, y + 9, gx + 4, y + 13, withAlpha(CYAN, 0xC0));
		g.fill(gx + 7, y + 9, gx + 11, y + 13, withAlpha(MAGENTA, 0xC0));
		g.fill(gx + 14, y + 9, gx + 18, y + 13, withAlpha(LIME, (int) (0x50 + 0xAF * pulse(260))));
	}

	/** A smaller panel without the header band (crafting guide, HUD pop-ups, tips). */
	public static void smallPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, int accent) {
		chamfer(g, x - 2, y - 2, w + 4, h + 4, 8, withAlpha(accent, 0x30));
		chamfer(g, x, y, w, h, 6, accent);
		chamfer(g, x + 1, y + 1, w - 2, h - 2, 5, GLASS);
		for (int sy = y + 3; sy < y + h - 3; sy += 3) g.fill(x + 2, sy, x + w - 2, sy + 1, 0x07FFFFFF);
	}

	/** A recessed screen inside a panel, used for recipes and tip pictures. */
	public static void display(GuiGraphicsExtractor g, int x, int y, int w, int h, int accent) {
		chamfer(g, x, y, w, h, 5, withAlpha(accent, 0x90));
		chamfer(g, x + 1, y + 1, w - 2, h - 2, 4, 0xFF050714);
		for (int sy = y + 2; sy < y + h - 2; sy += 2) g.fill(x + 2, sy, x + w - 2, sy + 1, withAlpha(accent, 0x0A));
	}

	public static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) { g.text(font, s, x, y, color, false); }
	public static void text(GuiGraphicsExtractor g, Font font, FormattedCharSequence s, int x, int y, int color) { g.text(font, s, x, y, color, false); }
	public static void textCentered(GuiGraphicsExtractor g, Font font, String s, int cx, int y, int color) { text(g, font, s, cx - font.width(s) / 2, y, color); }

	/** An item drawn at 2x (or any) scale. */
	public static void bigItem(GuiGraphicsExtractor g, Item item, int x, int y, float scale) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.item(new ItemStack(item), 0, 0);
		g.pose().popMatrix();
	}

	/** Text drawn at a larger scale (used for glyphs in tip pictures). */
	public static void bigText(GuiGraphicsExtractor g, Font font, String s, int cx, int cy, float scale, int color) {
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().scale(scale, scale);
		g.text(font, s, -font.width(s) / 2, -4, color, false);
		g.pose().popMatrix();
	}

	// ---- recipe grid (2x2 for the inventory, 3x3 for the crafting table)
	public static int gridSize(Item[] grid) { return grid.length == 4 ? 2 : 3; }
	public static int recipeW(Item[] grid) { return gridSize(grid) * 19 + 24 + 19; }
	public static int recipeH(Item[] grid) { return gridSize(grid) * 19; }

	public static void slot(GuiGraphicsExtractor g, int x, int y, int accent) {
		g.fill(x, y, x + 18, y + 18, withAlpha(accent, 0x70));
		g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF0E1328);
	}

	public static void recipe(GuiGraphicsExtractor g, Font font, int x, int y, Item[] grid, Item result, int accent) {
		int n = gridSize(grid);
		for (int i = 0; i < grid.length; i++) {
			int sx = x + (i % n) * 19, sy = y + (i / n) * 19;
			slot(g, sx, sy, accent);
			if (grid[i] != null) g.item(new ItemStack(grid[i]), sx + 1, sy + 1);
		}
		int mid = y + (n * 19) / 2;
		text(g, font, "▶", x + n * 19 + 8, mid - 4, accent);
		slot(g, x + n * 19 + 24, mid - 9, GREEN);
		g.item(new ItemStack(result), x + n * 19 + 25, mid - 8);
	}

	/** A segmented "digital" progress bar. */
	public static void progressBar(GuiGraphicsExtractor g, int x, int y, int w, int current, int total, int accent) {
		int gap = 2, seg = (w - gap * (total - 1)) / total;
		float p = pulse(180);
		for (int i = 1; i <= total; i++) {
			int sx = x + (i - 1) * (seg + gap);
			int c = i < current ? accent : i == current ? withAlpha(WHITE, (int) (0x90 + 0x6F * p)) : withAlpha(accent, 0x30);
			g.fill(sx, y, sx + seg, y + 4, c);
		}
	}

	// ---- checkmark, drawn cell by cell
	private static final int[][] CHECK = {
		{0, 5}, {1, 6}, {2, 7}, {3, 8}, {4, 7}, {5, 6}, {6, 5}, {7, 4}, {8, 3}, {9, 2}, {10, 1}, {11, 0}
	};

	public static void check(GuiGraphicsExtractor g, int x, int y, int scale, float progress, int color) {
		int shown = Math.round(CHECK.length * Math.max(0, Math.min(1, progress)));
		for (int i = 0; i < shown; i++) {
			int cx = x + CHECK[i][0] * scale, cy = y + CHECK[i][1] * scale;
			g.fill(cx - 1, cy - 1, cx + scale + 1, cy + scale + 1, withAlpha(color, 0x40));
			g.fill(cx, cy, cx + scale, cy + scale, color);
		}
	}

	public static int checkW(int scale) { return 12 * scale; }
	public static int checkH(int scale) { return 9 * scale; }

	public static java.util.List<FormattedCharSequence> wrap(Font font, String s, int width) {
		return font.split(Component.literal(s), width);
	}
}
