package com.findyourpath.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block Guide: when you mine one of the 50 most common blocks, a card pops up showing the block,
 * what it is, and what it's good for. "Close" means it can come back later (after a 10-minute rest);
 * "Don't remind me" turns it off for that block for good (saved in config/findyourpath.json).
 */
public final class BlockGuide {
	private BlockGuide() {}

	/** One card. {@code key} is what gets muted; several block ids can share a card (e.g. both iron ores). */
	public record Fact(String key, String name, String what, String use) {}

	private static final Map<String, Fact> BY_BLOCK = new HashMap<>();
	private static final long REST_MS = 10 * 60 * 1000;
	private static final Map<String, Long> restingUntil = new HashMap<>();

	private static Fact pending;
	private static Item pendingIcon;
	private static String pendingTitle;

	private static void add(String name, String what, String use, String... ids) {
		Fact f = new Fact(ids[0], name, what, use);
		for (String id : ids) BY_BLOCK.put("minecraft:" + id, f);
	}

	static {
		// --- everyday ground and stone
		add("Stone", "The most common block underground. Mining it drops cobblestone.",
				"Smelt cobblestone back into stone, or craft stone bricks, stairs and slabs.", "stone");
		add("Deepslate", "The darker, tougher stone found deep down (below about y=0). It's slower to mine.",
				"Gives cobbled deepslate: great for building, tools, and furnaces.", "deepslate");
		add("Cobblestone", "Rough stone you get from mining stone.",
				"Stone tools, furnaces, stairs, walls, and sturdy first houses.", "cobblestone", "cobbled_deepslate");
		add("Granite", "A pinkish stone found in big patches underground.",
				"Decoration. Polish 4 into polished granite for a smooth look.", "granite");
		add("Diorite", "A white, speckled stone found in patches underground.",
				"Decoration, or mix with cobblestone to make andesite.", "diorite");
		add("Andesite", "A gray stone found in patches underground.",
				"Decoration. Polished andesite makes clean floors.", "andesite");
		add("Tuff", "A gray-green stone found deep underground, near deepslate.",
				"Building. Craft tuff bricks or chiseled tuff for detail.", "tuff");
		add("Dirt", "Plain soil. Easy to dig with any tool, or your hands.",
				"Quick shelters, filling holes, and planting trees and grass.", "dirt", "coarse_dirt", "rooted_dirt");
		add("Grass Block", "Dirt with grass on top. It drops dirt unless you use Silk Touch.",
				"Grass spreads to nearby dirt. Animals spawn on it. Till it with a hoe to farm.", "grass_block", "podzol", "mycelium");
		add("Sand", "Loose sand found on beaches and in deserts. It falls if nothing is under it.",
				"Smelt into glass. Craft sandstone. Needed for TNT.", "sand");
		add("Red Sand", "Orange sand found in badlands (mesa) biomes.",
				"Smelts into glass, and crafts red sandstone.", "red_sand");
		add("Sandstone", "Sand pressed into stone. Found under deserts and beaches.",
				"Building, especially desert-style houses.", "sandstone", "red_sandstone");
		add("Gravel", "Loose gravel that falls like sand.",
				"Sometimes drops flint, for flint and steel and arrows.", "gravel");
		add("Clay", "Soft gray block found under shallow water and in lush caves.",
				"Drops clay balls: smelt into bricks, or make terracotta.", "clay");
		add("Mud", "Wet dirt found in swamps.",
				"Mix with wheat for packed mud, then make mud bricks.", "mud");
		add("Snow", "Snow on the ground in cold biomes. Needs a shovel to collect.",
				"Snowballs to throw, snow blocks, and snow golems.", "snow", "snow_block");
		add("Ice", "Frozen water. It breaks into water unless you use Silk Touch.",
				"Slippery paths and boat roads. Packed ice doesn't melt.", "ice");

		// --- wood
		String logWhat = "A log from a tree. Punch it or use an axe.";
		String logUse = "Turn into planks (4 per log), then sticks, tools, crafting tables and chests. Smelt into charcoal.";
		add("Oak Log", logWhat, logUse, "oak_log");
		add("Birch Log", logWhat, logUse, "birch_log");
		add("Spruce Log", logWhat, logUse, "spruce_log");
		add("Jungle Log", logWhat, logUse, "jungle_log");
		add("Acacia Log", logWhat, logUse, "acacia_log");
		add("Dark Oak Log", logWhat, logUse, "dark_oak_log");
		add("Cherry Log", logWhat, logUse, "cherry_log");
		add("Mangrove Log", logWhat, logUse, "mangrove_log");
		add("Planks", "Wood boards made from logs.",
				"The base of almost everything early: sticks, tools, crafting tables, chests, doors, boats.",
				"oak_planks", "birch_planks", "spruce_planks", "jungle_planks", "acacia_planks",
				"dark_oak_planks", "cherry_planks", "mangrove_planks", "bamboo_planks", "pale_oak_planks");
		add("Leaves", "Leaves from a tree. They disappear on their own once the log is gone.",
				"Sometimes drop saplings (plant more trees), sticks, and from oaks, apples.",
				"oak_leaves", "birch_leaves", "spruce_leaves", "jungle_leaves", "acacia_leaves",
				"dark_oak_leaves", "cherry_leaves", "mangrove_leaves", "azalea_leaves", "flowering_azalea_leaves", "pale_oak_leaves");
		add("Grass", "Short plants growing on grass blocks.",
				"Sometimes drops wheat seeds, so you can start a farm.", "short_grass", "tall_grass", "fern", "large_fern");

		// --- ores
		add("Coal Ore", "Stone with black coal in it. Common, often near the surface.",
				"Coal fuels furnaces and makes torches.", "coal_ore", "deepslate_coal_ore");
		add("Iron Ore", "Stone with iron in it. Needs a stone pickaxe or better.",
				"Smelt raw iron into ingots: armor, tools, buckets, shields, rails.", "iron_ore", "deepslate_iron_ore");
		add("Copper Ore", "Stone with orange-green copper in it. Needs a stone pickaxe or better.",
				"Smelt into ingots: copper blocks, lightning rods, spyglasses, copper tools.", "copper_ore", "deepslate_copper_ore");
		add("Gold Ore", "Stone with gold in it. Needs an iron pickaxe or better.",
				"Smelt into ingots: golden apples, clocks, powered rails. Piglins love gold.", "gold_ore", "deepslate_gold_ore");
		add("Redstone Ore", "Stone with red dust in it. Glows when touched. Needs an iron pickaxe.",
				"Redstone dust is the wiring for doors, lamps, traps and machines.", "redstone_ore", "deepslate_redstone_ore");
		add("Lapis Lazuli Ore", "Stone with blue lapis in it. Needs a stone pickaxe or better.",
				"Lapis powers the enchanting table. Also blue dye.", "lapis_ore", "deepslate_lapis_ore");
		add("Diamond Ore", "Stone with diamonds in it. Rare and deep. Needs an iron pickaxe.",
				"The best early tools and armor, enchanting tables, and jukeboxes.", "diamond_ore", "deepslate_diamond_ore");
		add("Emerald Ore", "A rare ore found in mountains. Needs an iron pickaxe.",
				"Emeralds are money for trading with villagers.", "emerald_ore", "deepslate_emerald_ore");
		add("Obsidian", "Very hard black block made when water meets lava. Needs a diamond pickaxe.",
				"Nether portals, enchanting tables, and blast-proof walls.", "obsidian");

		// --- nether and end
		add("Netherrack", "The red stone that makes up most of the Nether. Breaks very fast.",
				"Cheap building in the Nether. Set it on fire and it burns forever (great fireplaces).", "netherrack");
		add("Nether Quartz Ore", "Netherrack with white quartz in it.",
				"Quartz makes comparators, observers, and smooth white quartz blocks. Gives lots of XP.", "nether_quartz_ore");
		add("Nether Gold Ore", "Netherrack with flecks of gold.",
				"Drops gold nuggets. 9 nuggets make 1 gold ingot.", "nether_gold_ore");
		add("Ancient Debris", "The rarest block in the Nether, deep down. Needs a diamond pickaxe. Lava can't burn it.",
				"Smelt into netherite scrap, then make netherite to upgrade diamond gear.", "ancient_debris");
		add("Soul Sand", "Brown sand in the Nether that slows you down.",
				"Grows nether wart for potions. Makes bubble elevators in water. Soul torches and fire.", "soul_sand", "soul_soil");
		add("Basalt", "Gray rock with stripes, found in the Nether.",
				"Building. Polished basalt looks like pillars.", "basalt");
		add("Blackstone", "Dark stone from the Nether. Works like cobblestone.",
				"Stone tools, furnaces, and polished blackstone bricks.", "blackstone");
		add("Glowstone", "A glowing block that hangs from the Nether ceiling.",
				"Bright light, and glowstone dust makes potions stronger.", "glowstone");
		add("End Stone", "The pale yellow stone of the End.",
				"End stone bricks. Blast-resistant, so dragon-proof building.", "end_stone");

		// --- plants and placed blocks
		add("Sugar Cane", "Tall green stalks that grow next to water.",
				"Paper (for books and maps) and sugar (for cake).", "sugar_cane");
		add("Pumpkin", "A big orange gourd found in grassy areas.",
				"Pumpkin pie, jack o'lanterns, iron and snow golems. Wear a carved one to look at Endermen safely.", "pumpkin");
		add("Melon", "A big green fruit block found in jungles.",
				"Drops melon slices to eat. Craft seeds to grow more.", "melon");
		add("Crafting Table", "The 3x3 workbench. You probably placed this one.",
				"Crafting almost every tool, block and item in the game.", "crafting_table");
		add("Furnace", "A stone oven for cooking and smelting.",
				"Cooks food, smelts ores into ingots, sand into glass.", "furnace");
		add("Chest", "A wooden box with 27 slots. Mining it spills out whatever is inside.",
				"Storage. Put two side by side for a big chest.", "chest");
		add("Torch", "A stick with fire on top.",
				"Light. Monsters can't spawn in well-lit places.", "torch", "wall_torch");
	}

	/** Called right after the player breaks a block. */
	public static void onBlockMined(BlockState state) {
		Block block = state.getBlock();
		Fact f = BY_BLOCK.get(BuiltInRegistries.BLOCK.getKey(block).toString());
		if (f == null || FindYourPathClient.blockGuideMuted(f.key())) return;
		Long until = restingUntil.get(f.key());
		if (until != null && System.currentTimeMillis() < until) return;
		Item icon = block.asItem();
		if (icon == Items.AIR) icon = iconFallback(f);
		pending = f;
		pendingIcon = icon;
		pendingTitle = block.getName().getString();
		// shown on the next tick once no other screen is open
	}

	private static Item iconFallback(Fact f) {
		if (f.key().equals("torch")) return Items.TORCH;
		if (f.key().equals("snow")) return Items.SNOW_BLOCK;
		return Items.STONE;
	}

	/** Runs every client tick. */
	public static void tick(Minecraft mc) {
		if (pending == null || mc.gui.screen() != null) return;
		Fact f = pending;
		Item icon = pendingIcon;
		String title = pendingTitle;
		pending = null;
		// mark it resting now, so mining a whole vein doesn't keep popping it
		restingUntil.put(f.key(), System.currentTimeMillis() + REST_MS);
		mc.gui.setScreen(new TabletScreen()
				.header("Block Guide", Ui.CYAN)
				.heading(icon, title, Ui.WHITE)
				.para("WHAT IT IS", Ui.CYAN)
				.para(f.what())
				.para("USE IT FOR", Ui.LIME)
				.para(f.use())
				.button("Close", () -> mc.gui.setScreen(null))
				.button("Don't remind me again for " + f.name(), () -> {
					FindYourPathClient.muteBlockGuide(f.key());
					mc.gui.setScreen(null);
				}));
	}
}
