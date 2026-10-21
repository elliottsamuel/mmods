package com.findyourpath.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.findyourpath.client.Quests.Role;
import com.findyourpath.client.Quests.Step;
import com.findyourpath.client.mixin.ContainerScreenAccessor;

/**
 * Live coaching inside the inventory and crafting table: reads the grid every frame,
 * makes the squares you still need to fill glow (with a faded "ghost" of the item),
 * marks wrong items red, and tells you the very next move.
 */
public final class CraftingGuide {
	private CraftingGuide() {}

	public static boolean isCraftingScreen(Screen s) {
		return s instanceof InventoryScreen || s instanceof CraftingScreen;
	}

	private enum Mark { NONE, NEED, WRONG }

	// ---- ingredient helpers
	private static boolean matches(Item want, ItemStack have) {
		if (have.isEmpty()) return false;
		if (want == Items.OAK_PLANKS) return have.is(ItemTags.PLANKS);
		if (want == Items.OAK_LOG) return have.is(ItemTags.LOGS);
		return have.is(want);
	}

	private static String name(Item want, int n) {
		if (want == Items.OAK_PLANKS) return n == 1 ? "plank" : "planks";
		if (want == Items.OAK_LOG) return n == 1 ? "log" : "logs";
		if (want == Items.STICK) return n == 1 ? "stick" : "sticks";
		if (want == Items.COBBLESTONE) return "cobblestone";
		return new ItemStack(want).getHoverName().getString().toLowerCase();
	}

	private static String howToGetMore(Item want) {
		if (want == Items.OAK_PLANKS) return "You need more planks. Put a log in the grid first to make 4.";
		if (want == Items.OAK_LOG) return "You need a log. Close this and chop a tree.";
		if (want == Items.STICK) return "You need sticks. Make them first: 2 planks stacked on top of each other.";
		if (want == Items.COBBLESTONE) return "You need more cobblestone. Mine gray stone with your pickaxe.";
		return "You need more " + name(want, 2) + ".";
	}

	/** Everything we know about the grid right now, plus the coaching line. */
	private record Reading(String message, boolean showResult, Mark[] marks, int[] gridSlots, Item[] want) {}

	private static Reading read(AbstractContainerScreen<?> screen, Step st, Tracker tracker) {
		AbstractContainerMenu menu = screen.getMenu();
		boolean table = screen instanceof CraftingScreen;
		int n = table ? 3 : 2;

		if (st.recipe() == null) {
			String msg = st.title().startsWith("Place your crafting table") || st.title().startsWith("Place your door") || st.title().equals("Build walls")
					? "Close this screen (press E) so you can place blocks in the world."
					: "Close this screen (press E) to do this step out in the world.";
			return new Reading(msg, false, null, null, null);
		}
		int size = Ui.gridSize(st.recipe());
		if (size > n) {
			return new Reading("This one needs the bigger 3x3 grid. Press E to close, then RIGHT-CLICK your crafting table.", false, null, null, null);
		}

		// map recipe cells onto the menu's grid slots (slot 0 is the result, then the grid row by row)
		int[] gridSlots = new int[size * size];
		Item[] want = new Item[size * size];
		for (int r = 0; r < size; r++) {
			for (int c = 0; c < size; c++) {
				gridSlots[r * size + c] = 1 + r * n + c;
				want[r * size + c] = st.recipe()[r * size + c];
			}
		}
		Mark[] marks = new Mark[n * n + 1];
		java.util.Arrays.fill(marks, Mark.NONE);
		int wrong = 0, missing = 0;
		Item missingItem = null;
		// cells outside the recipe area must be empty
		for (int i = 1; i <= n * n; i++) {
			boolean inRecipe = false;
			for (int g : gridSlots) if (g == i) inRecipe = true;
			if (!inRecipe && !menu.getSlot(i).getItem().isEmpty()) { marks[i] = Mark.WRONG; wrong++; }
		}
		for (int k = 0; k < gridSlots.length; k++) {
			ItemStack have = menu.getSlot(gridSlots[k]).getItem();
			if (want[k] == null) {
				if (!have.isEmpty()) { marks[gridSlots[k]] = Mark.WRONG; wrong++; }
			} else if (have.isEmpty()) {
				marks[gridSlots[k]] = Mark.NEED; missing++; missingItem = want[k];
			} else if (!matches(want[k], have)) {
				marks[gridSlots[k]] = Mark.WRONG; wrong++;
			}
		}

		ItemStack result = menu.getSlot(0).getItem();
		ItemStack carried = menu.getCarried();
		if (matches(st.icon(), carried)) {
			return new Reading("Got it! Now click an empty spot in your inventory to put it away.", false, null, null, null);
		}
		// any valid arrangement counts: if the result is already there, just take it
		if (matches(st.icon(), result)) {
			return new Reading("It worked! Click the " + name(st.icon(), 1) + " on the right to take it.", true, null, null, null);
		}
		if (wrong > 0) {
			return new Reading("Take the item out of the red square: click it, then click an empty spot in your inventory.", false, marks, gridSlots, want);
		}
		if (missing == 0 && !result.isEmpty()) {
			return new Reading("It worked! Click the " + name(st.icon(), 1) + " on the right to take it.", true, marks, gridSlots, want);
		}
		if (missing > 0) {
			// do they have enough of the ingredient (inventory + hand)?
			int have = tracker.countMatching(missingItem);   // includes what's on the cursor
			if (have < missing) return new Reading(howToGetMore(missingItem), false, marks, gridSlots, want);
			if (matches(missingItem, carried)) {
				return new Reading("RIGHT-CLICK each glowing square to drop one " + name(missingItem, 1) + ". " + missing + " to go.", false, marks, gridSlots, want);
			}
			if (!carried.isEmpty()) {
				return new Reading("Put down what you're holding: click an empty spot in your inventory.", false, marks, gridSlots, want);
			}
			return new Reading("Click your " + name(missingItem, 2) + " in the inventory below to pick them up. Then fill the glowing squares.", false, marks, gridSlots, want);
		}
		return new Reading("Almost! Check the pattern matches the glowing squares.", false, marks, gridSlots, want);
	}

	/** Draws the glow/ghost/red marks on the real grid, and the coaching panel in the corner. */
	public static void draw(Screen screen, GuiGraphicsExtractor g, Role role, int stepIndex, Tracker tracker, boolean justDone) {
		if (!(screen instanceof AbstractContainerScreen<?> acs)) return;
		Step st = role.steps().get(stepIndex);
		Reading rd = read(acs, st, tracker);
		Font font = Minecraft.getInstance().font;
		ContainerScreenAccessor pos = (ContainerScreenAccessor) acs;
		int left = pos.findyourpath$leftPos(), top = pos.findyourpath$topPos();
		AbstractContainerMenu menu = acs.getMenu();
		float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 220.0));

		if (rd.marks() != null) {
			for (int k = 0; k < rd.gridSlots().length; k++) {
				int si = rd.gridSlots()[k];
				Slot s = menu.getSlot(si);
				int x = left + s.x, y = top + s.y;
				if (rd.marks()[si] == Mark.NEED) {
					g.item(new ItemStack(rd.want()[k]), x, y);
					g.fill(x, y, x + 16, y + 16, 0xA88B8B8B);                         // fade it into a ghost
					int a = (int) (0x40 + 0x70 * pulse);
					g.fill(x - 1, y - 1, x + 17, y + 17, (a << 24) | 0xFFD24A);        // glow
				}
			}
			for (int i = 1; i < rd.marks().length; i++) {
				if (rd.marks()[i] == Mark.WRONG) {
					Slot s = menu.getSlot(i);
					g.fill(left + s.x - 1, top + s.y - 1, left + s.x + 17, top + s.y + 17, 0x88FF3030);
				}
			}
		}
		if (rd.showResult()) {
			Slot s = menu.getSlot(0);
			int a = (int) (0x50 + 0x80 * pulse);
			g.fill(left + s.x - 2, top + s.y - 2, left + s.x + 18, top + s.y + 18, (a << 24) | 0x55FF55);
		}

		// coaching panel
		int pw = 140, x = 6, y = 6, pad = 10;
		List<FormattedCharSequence> title = Ui.wrap(font, st.title(), pw - pad * 2);
		List<FormattedCharSequence> msg = Ui.wrap(font, rd.message(), pw - pad * 2);
		int ph = pad + 11 + 8 + (justDone ? 11 : 0) + title.size() * 10 + 5 + msg.size() * 10 + pad;
		Ui.smallPanel(g, x, y, pw, ph, role.color());
		int cy = y + pad;
		if (justDone) {
			Ui.text(g, font, "✔ Step done!", x + pad, cy, Ui.GREEN);
			cy += 11;
		}
		Ui.text(g, font, role.name().toUpperCase() + "  " + (stepIndex + 1) + "/" + role.steps().size(), x + pad, cy, role.color());
		cy += 11;
		Ui.progressBar(g, x + pad, cy, pw - pad * 2, stepIndex + 1, role.steps().size(), role.color());
		cy += 8;
		for (FormattedCharSequence l : title) { Ui.text(g, font, l, x + pad, cy, Ui.WHITE); cy += 10; }
		cy += 5;
		for (FormattedCharSequence l : msg) { Ui.text(g, font, l, x + pad, cy, Ui.SOFT); cy += 10; }
	}
}
