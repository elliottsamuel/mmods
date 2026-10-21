package com.findyourpath.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Drawing helpers for the carved-stone look: tablet art, engraved text, carved slots, notches and the carved checkmark. */
public final class Ui {
	private Ui() {}

	// bright colors, for text drawn over the world (HUD)
	public static final int WHITE = 0xFFFFFFFF;
	public static final int SOFT = 0xFFE0E0E0;
	public static final int MUTED = 0xFFB8B8B8;
	public static final int GOLD = 0xFFFFAA00;
	public static final int GREEN = 0xFF55FF55;

	// ink colors, for text engraved into stone
	public static final int INK = 0xFF26241F;
	public static final int INK_SOFT = 0xFF3A3731;
	public static final int INK_MUTED = 0xFF57534B;
	public static final int ENGRAVE_LIGHT = 0xFFBDBBB2;
	public static final int GOLD_INLAY = 0xFFC9962E;

	public static final Identifier TABLET = FindYourPathClient.id("tablet");
	public static final Identifier TABLET_SMALL = FindYourPathClient.id("tablet_small");
	public static final Identifier PLAQUE = FindYourPathClient.id("plaque");
	public static final Identifier BUTTON = FindYourPathClient.id("button");
	public static final Identifier BUTTON_HOVER = FindYourPathClient.id("button_hover");
	public static final Identifier SLOT = FindYourPathClient.id("slot");

	public static void sprite(GuiGraphicsExtractor g, Identifier id, int x, int y, int w, int h) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h);
	}

	public static void tablet(GuiGraphicsExtractor g, int x, int y, int w, int h) { sprite(g, TABLET, x, y, w, h); }
	public static void smallTablet(GuiGraphicsExtractor g, int x, int y, int w, int h) { sprite(g, TABLET_SMALL, x, y, w, h); }

	/** Bright UI colors become darker "ink" so they read as carved into the stone. */
	public static int ink(int color) {
		if (color == WHITE) return INK;
		if (color == SOFT) return INK_SOFT;
		if (color == MUTED) return INK_MUTED;
		int r = (color >> 16) & 0xFF, gr = (color >> 8) & 0xFF, b = color & 0xFF;
		return 0xFF000000 | ((int) (r * .45) << 16) | ((int) (gr * .45) << 8) | (int) (b * .45);
	}

	/** Engraved text: dark letters with a pale lip just below, like a chisel cut catching light. */
	public static void carved(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) {
		g.text(font, s, x, y + 1, ENGRAVE_LIGHT, false);
		g.text(font, s, x, y, color, false);
	}

	public static void carved(GuiGraphicsExtractor g, Font font, FormattedCharSequence s, int x, int y, int color) {
		g.text(font, s, x, y + 1, ENGRAVE_LIGHT, false);
		g.text(font, s, x, y, color, false);
	}

	public static void carvedCentered(GuiGraphicsExtractor g, Font font, String s, int cx, int y, int color) {
		carved(g, font, s, cx - font.width(s) / 2, y, color);
	}

	public static void carvedCentered(GuiGraphicsExtractor g, Font font, FormattedCharSequence s, int cx, int y, int color) {
		carved(g, font, s, cx - font.width(s) / 2, y, color);
	}

	// ---- recipe grid (2x2 for the inventory, 3x3 for the crafting table)
	public static int gridSize(Item[] grid) { return grid.length == 4 ? 2 : 3; }
	public static int recipeW(Item[] grid) { return gridSize(grid) * 18 + 22 + 18; }
	public static int recipeH(Item[] grid) { return gridSize(grid) * 18; }

	public static void recipe(GuiGraphicsExtractor g, Font font, int x, int y, Item[] grid, Item result) {
		int n = gridSize(grid);
		for (int i = 0; i < grid.length; i++) {
			int sx = x + (i % n) * 18, sy = y + (i / n) * 18;
			sprite(g, SLOT, sx, sy, 18, 18);
			if (grid[i] != null) g.item(new ItemStack(grid[i]), sx + 1, sy + 1);
		}
		int mid = y + (n * 18) / 2;
		carved(g, font, "→", x + n * 18 + 7, mid - 4, INK);
		sprite(g, SLOT, x + n * 18 + 22, mid - 9, 18, 18);
		g.item(new ItemStack(result), x + n * 18 + 23, mid - 8);
	}

	/** Notches chiselled into the stone: gold-filled when done, pale when current, dark when still to do. */
	public static void notches(GuiGraphicsExtractor g, int x, int y, int current, int total) {
		for (int i = 1; i <= total; i++) {
			int px = x + (i - 1) * 9;
			g.fill(px, y, px + 6, y + 1, 0xFF3A3833);
			g.fill(px, y, px + 1, y + 6, 0xFF3A3833);
			g.fill(px + 1, y + 6, px + 7, y + 7, ENGRAVE_LIGHT);
			g.fill(px + 6, y + 1, px + 7, y + 7, ENGRAVE_LIGHT);
			int c = i < current ? GOLD_INLAY : i == current ? 0xFFE9E3C8 : 0xFF5F5C55;
			g.fill(px + 1, y + 1, px + 6, y + 6, c);
		}
	}

	public static int notchesWidth(int total) { return total * 9 - 2; }

	// ---- carved checkmark, drawn stroke by stroke
	private static final int[][] CHECK = {
		{0, 5}, {1, 6}, {2, 7}, {3, 8}, {4, 7}, {5, 6}, {6, 5}, {7, 4}, {8, 3}, {9, 2}, {10, 1}, {11, 0}
	};

	/** Draws the first {@code progress} (0..1) of the checkmark, each cell {@code scale} px, as a groove with a gold inlay. */
	public static void carvedCheck(GuiGraphicsExtractor g, int x, int y, int scale, float progress) {
		int shown = Math.round(CHECK.length * Math.max(0, Math.min(1, progress)));
		for (int i = 0; i < shown; i++) {
			int cx = x + CHECK[i][0] * scale, cy = y + CHECK[i][1] * scale;
			g.fill(cx, cy, cx + scale + 1, cy + scale + 1, 0xFF2E2C27);
			g.fill(cx + 1, cy + 1, cx + scale, cy + scale, GOLD_INLAY);
			g.fill(cx + 1, cy + scale, cx + scale + 1, cy + scale + 1, ENGRAVE_LIGHT);
		}
	}

	public static int checkW(int scale) { return 12 * scale + 1; }
	public static int checkH(int scale) { return 9 * scale + 1; }

	public static java.util.List<FormattedCharSequence> wrap(Font font, String s, int width) {
		return font.split(Component.literal(s), width);
	}
}
