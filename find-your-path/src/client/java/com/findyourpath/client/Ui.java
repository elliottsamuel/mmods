package com.findyourpath.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Drawing helpers: the stone tablet, the recipe grid, progress pips. Plain colored shapes only, no textures. */
public final class Ui {
	private Ui() {}

	public static final int WHITE = 0xFFFFFFFF;
	public static final int SOFT = 0xFFE0E0E0;
	public static final int MUTED = 0xFFB8B8B8;
	public static final int GOLD = 0xFFFFAA00;
	public static final int GREEN = 0xFF55FF55;

	/** A carved stone tablet: dark outline, beveled rim, speckled stone face. */
	public static void tablet(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		g.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0xFF141414);      // outline
		g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF3A3A3A);      // rim
		g.fill(x, y, x + w, y + h, 0xFF5E5E5E);                      // face
		// speckles, same pattern every frame
		long seed = 0x5DEECE66DL;
		for (int py = y + 2; py < y + h - 4; py += 4) {
			for (int px = x + 2; px < x + w - 4; px += 4) {
				seed = seed * 6364136223846793005L + 1442695040888963407L;
				int r = (int) ((seed >>> 33) & 15);
				if (r < 3) g.fill(px, py, px + 4, py + 4, 0xFF555555);
				else if (r < 5) g.fill(px, py, px + 4, py + 4, 0xFF686868);
			}
		}
		// bevel: light top/left, dark bottom/right
		g.fill(x, y, x + w, y + 2, 0xFF8A8A8A);
		g.fill(x, y, x + 2, y + h, 0xFF8A8A8A);
		g.fill(x, y + h - 2, x + w, y + h, 0xFF3F3F3F);
		g.fill(x + w - 2, y, x + w, y + h, 0xFF3F3F3F);
	}

	/** A recessed panel inside the tablet, used behind the recipe. */
	public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, 0xFF4A4A4A);
		g.fill(x, y, x + w, y + 1, 0xFF333333);
		g.fill(x, y, x + 1, y + h, 0xFF333333);
		g.fill(x, y + h - 1, x + w, y + h, 0xFF7A7A7A);
		g.fill(x + w - 1, y, x + w, y + h, 0xFF7A7A7A);
	}

	/** One inventory-style slot, 18x18. */
	public static void slot(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
		g.fill(x, y, x + 17, y + 1, 0xFF373737);
		g.fill(x, y, x + 1, y + 17, 0xFF373737);
		g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
		g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
	}

	public static final int RECIPE_W = 3 * 18 + 22 + 18, RECIPE_H = 3 * 18;

	/** 3x3 crafting grid with an arrow and the result. */
	public static void recipe(GuiGraphicsExtractor g, Font font, int x, int y, Item[] grid, Item result) {
		for (int i = 0; i < 9; i++) {
			int sx = x + (i % 3) * 18, sy = y + (i / 3) * 18;
			slot(g, sx, sy);
			if (grid[i] != null) g.item(new ItemStack(grid[i]), sx + 1, sy + 1);
		}
		g.text(font, "→", x + 3 * 18 + 6, y + 23, WHITE, true);
		slot(g, x + 3 * 18 + 22, y + 18);
		g.item(new ItemStack(result), x + 3 * 18 + 23, y + 19);
	}

	/** Small squares showing progress: green done, yellow current, dark to do. */
	public static void pips(GuiGraphicsExtractor g, int x, int y, int current, int total) {
		for (int i = 1; i <= total; i++) {
			int c = i < current ? 0xFF55FF55 : i == current ? 0xFFFFFF55 : 0xFF2E2E2E;
			int px = x + (i - 1) * 8;
			g.fill(px, y, px + 6, y + 6, 0xFF141414);
			g.fill(px + 1, y + 1, px + 5, y + 5, c);
		}
	}

	public static int pipsWidth(int total) { return total * 8 - 2; }
}
