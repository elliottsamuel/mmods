package com.findyourpath.client;

import java.util.List;
import java.util.function.Predicate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * All the content.
 * Level 1, "Survive Your First Day", teaches the basics and unlocks the five paths.
 * The paths are Level 2: they build on Level 1 and ask you to remember what you learned (recipes hide behind "Show me how").
 * Side lessons fill the wait before nightfall.
 */
public final class Quests {
	private Quests() {}

	/**
	 * A step. {@code skippable}: a "Skip this step" button appears. {@code recall}: instructions and recipe stay hidden
	 * until the player taps "Show me how", so they can test what they remember.
	 */
	public record Step(String title, String howTo, String tip, String bar, Item icon, Item[] recipe, Check check,
			boolean skippable, boolean recall, int minutes) {
		Step asSkippable() { return new Step(title, howTo, tip, bar, icon, recipe, check, true, recall, minutes); }
		Step asRecall() { return new Step(title, howTo, tip, bar, icon, recipe, check, skippable, true, minutes); }
	}

	public interface Check extends Predicate<Tracker> {}

	public record Role(String id, String name, int color, Item icon, String tagline, List<String> about,
			String goal, List<Step> steps, List<String> next, int minutes) {}

	/** One element of a tip picture: either an item drawn big, or a glyph like an arrow. */
	public record Pic(Item item, String glyph, int color) {}
	static Pic i(Item item) { return new Pic(item, null, 0); }
	static Pic g(String glyph, int color) { return new Pic(null, glyph, color); }

	public record Tip(String title, String text, Item icon, List<Pic> picture) {}

	/** Looks an item up by name, for items whose code name changed between versions. */
	static Item item(String name, Item fallback) {
		try {
			Item it = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(name));
			return it == null || it == Items.AIR ? fallback : it;
		} catch (Exception e) {
			return fallback;
		}
	}

	static Step step(String title, String howTo, String tip, String bar, Item icon, Item[] recipe, Check check) {
		return new Step(title, howTo, tip, bar, icon, recipe, check, false, false, 0);
	}

	// --- recipe helpers (4 items = the 2x2 inventory grid, 9 items = the 3x3 crafting table; read left-to-right, top-to-bottom)
	private static final Item P = Items.OAK_PLANKS, S = Items.STICK, C = Items.COBBLESTONE, L = Items.OAK_LOG, I = Items.IRON_INGOT;
	static final Item W = item("white_wool", item("wool", Items.STRING));
	private static Item[] grid(Item... cells) { return cells; }
	static final Item BED = item("white_bed", item("bed", W));

	// =====================================================================================================
	// LEVEL 1: SURVIVE YOUR FIRST DAY
	// =====================================================================================================
	static final Step LOGS = step("Get 3 logs",
			"Walk up to a tree. Hold LEFT-CLICK on the trunk until a block breaks off, then walk over the little floating log to pick it up.",
			"It takes a few seconds with bare hands. Keep holding!",
			"Find a tree and get 3 pieces of wood", Items.OAK_LOG, null, t -> t.countLogs() >= 3);
	static final Step PLANKS = step("Turn logs into planks",
			"Press E to open your inventory. Drag a log into the small 2x2 crafting grid. Planks appear on the right. Click them to take them.",
			"One log makes 4 planks. Turn all 3 logs into planks.",
			"Press E and turn your logs into planks", Items.OAK_PLANKS, grid(L, null, null, null), t -> t.countPlanks() >= 4);
	static final Step TABLE = step("Make a crafting table",
			"Press E again. Put a plank in all 4 squares of the small grid. Take the crafting table that appears.",
			"The crafting table is your workbench. Almost everything is made on it.",
			"Turn 4 planks into a crafting table", Items.CRAFTING_TABLE, grid(P, P, P, P), t -> t.has(Items.CRAFTING_TABLE) || t.tableNearby());
	static final Step PLACE = step("Place your crafting table",
			"Close your inventory. Pick the table in your hotbar (number keys 1-9, or scroll). Look at the ground and RIGHT-CLICK.",
			"Then RIGHT-CLICK the table to open its bigger 3x3 grid.",
			"Place your crafting table on the ground", Items.CRAFTING_TABLE, null, Tracker::tableNearby);
	static final Step STICKS = step("Make sticks",
			"Open your crafting table. Put one plank directly on top of another. You get 4 sticks.",
			"Sticks are the handles for tools and torches.",
			"Make sticks from 2 planks", Items.STICK, grid(null, P, null, null, P, null, null, null, null), t -> t.has(Items.STICK));
	static final Step PICKAXE = step("Make a wooden pickaxe",
			"Open your crafting table. Top row: 3 planks. Then 2 sticks straight down the middle.",
			"Pickaxe first! It lets you dig stone, and stone makes much stronger tools.",
			"Make a wooden pickaxe", Items.WOODEN_PICKAXE, grid(P, P, P, null, S, null, null, S, null), t -> t.has(Items.WOODEN_PICKAXE));
	static final Step STONE = step("Mine 3 cobblestone",
			"Find gray stone, often on hillsides or just under the dirt. Hold your pickaxe and hold LEFT-CLICK on it.",
			"Without a pickaxe, stone drops nothing. That's why you made one.",
			"Mine 3 cobblestone with your pickaxe", Items.COBBLESTONE, null, t -> t.count(Items.COBBLESTONE) >= 3);
	static final Step SWORD = step("Make a stone sword",
			"At your crafting table: 2 cobblestone stacked in the middle column, 1 stick under them.",
			"Stone is much stronger than wood. Now you can defend yourself.",
			"Make a stone sword", Items.STONE_SWORD, grid(null, C, null, null, C, null, null, S, null), t -> t.has(Items.STONE_SWORD));
	static final Step TORCHES = step("Find coal and make torches",
			"Look for stone with black specks (coal ore) on cliffs and in caves. Mine it with your pickaxe. Then craft: coal on top of a stick makes 4 torches.",
			"Coal can be hard to find. If you can't spot any, it's OK to skip this step.",
			"Find coal and make torches", Items.TORCH, grid(null, Items.COAL, null, null, S, null, null, null, null), t -> t.has(Items.TORCH)).asSkippable();
	static final Step FOOD = step("Get some food",
			"Hit a cow, pig, chicken or sheep until it drops meat. You can also find apples by breaking oak leaves.",
			"Eat when your hunger bar (the drumsticks) gets low: hold the food and hold RIGHT-CLICK. No animals around? You can skip this.",
			"Get some food", Items.BEEF, null, Tracker::hasFood).asSkippable();
	static final Step SHELTER = step("Be inside when night falls",
			"Make a shelter before dark: dig into a hillside, or build walls and a roof. Get inside and fill in the gap behind you.",
			"Monsters appear in the dark. A roof over your head keeps you safe.",
			"Get under a roof before night", Items.OAK_DOOR, null, t -> t.isNight() && t.underRoof());
	static final Step MORNING = step("Survive until morning",
			"Stay inside until the sun comes up. Listen for monsters outside! If you have a bed, sleeping skips the night.",
			"You made it through your first day once the sun rises.",
			"Stay safe until morning", Items.CLOCK, null, Tracker::morningCame);

	public static final Role FIRST_DAY = new Role("firstday", "Survive Your First Day", 0xFF4FE3FF, Items.TORCH,
			"Level 1: the basics everyone needs.",
			List.of("Before you choose your path, learn what every Minecraft player needs: wood, tools, a weapon, light, food, and a safe place to spend the night.",
					"Finish your first day to unlock the five paths."),
			"Learn the basics and make it through your first night.",
			List.of(LOGS, PLANKS, TABLE, PLACE, STICKS, PICKAXE, STONE, SWORD, TORCHES, FOOD, SHELTER, MORNING),
			List.of("Choose a path: Builder, Explorer, Adventurer, Engineer or Homesteader"), 15);

	// =====================================================================================================
	// SIDE LESSONS (offered while waiting for night)
	// =====================================================================================================
	public static final List<Role> SIDE_LESSONS = List.of(
		new Role("side_chest", "Make a chest", 0xFFFFC547, Items.CHEST, "Keep your stuff safe.", List.of(),
			"Store your things so you don't lose them if you die.",
			List.of(step("Make a chest", "At your crafting table: fill every square except the middle with planks (8 planks).",
					"Chests keep things safe. When you die you drop what you carry, but not what's in a chest.", "Make a chest from 8 planks",
					Items.CHEST, grid(P, P, P, P, null, P, P, P, P), t -> t.has(Items.CHEST) || t.chestNearby())),
			List.of(), 2),
		new Role("side_furnace", "Cook your food", 0xFFFF8A3D, Items.FURNACE, "Cooked food fills you up more.", List.of(),
			"Build a furnace and cook something.",
			List.of(step("Make a furnace", "At your crafting table: fill every square except the middle with cobblestone (8 cobblestone).",
							"Need more cobblestone? Mine more stone with your pickaxe.", "Make a furnace from 8 cobblestone",
							Items.FURNACE, grid(C, C, C, C, null, C, C, C, C), t -> t.has(Items.FURNACE) || t.furnaceNearby()),
					step("Cook something", "Place the furnace and RIGHT-CLICK it. Put raw meat in the top slot and fuel (planks, logs or coal) in the bottom. Wait, then take the cooked food.",
							"Cooked food fills your hunger bar much more than raw food.", "Cook some food in your furnace",
							Items.COOKED_BEEF, null, Tracker::hasCookedFood)),
			List.of(), 4),
		new Role("side_bed", "Make a bed", 0xFFFF5A6E, BED, "Sleep through the night.", List.of(),
			"Make a bed so you can skip the night and set where you come back if you die.",
			List.of(step("Get 3 wool", "Find sheep. Hit one until it drops wool. Different sheep give different colors; any color works.",
							"If you have shears, RIGHT-CLICK a sheep to shear it instead.", "Get 3 wool from sheep",
							W, null, t -> t.countWool() >= 3),
					step("Make a bed", "At your crafting table: 3 wool across the top row, 3 planks across the middle row.",
							"Place the bed and RIGHT-CLICK it at night to sleep.", "Make a bed",
							BED, grid(W, W, W, P, P, P, null, null, null), Tracker::hasBed)),
			List.of(), 5)
	);

	// =====================================================================================================
	// LEVEL 2: THE FIVE PATHS
	// =====================================================================================================
	public static final List<Role> ROLES = List.of(
		new Role("builder", "Builder", 0xFFFFAA00, Items.BRICKS, "Make places: homes, towers, secret bases.",
			List.of("Builders make places. A cozy cabin, a castle on a cliff, a secret base under a lake. Almost every block can be picked up and placed somewhere else, so the whole world is your building material.",
					"Good fit if you like Legos, drawing, or designing spaces."),
			"Build a small stone house with a door and light.",
			List.of(
				step("Make a door", "At your crafting table, fill the left two columns with planks: 6 planks.",
						"One recipe gives you 3 doors.", "Make a door", Items.OAK_DOOR, grid(P, P, null, P, P, null, P, P, null), Tracker::hasDoor).asRecall(),
				step("Gather 20 cobblestone", "Mine stone with your pickaxe until you have 20 cobblestone.",
						"Stone is sturdier than dirt, and monsters can't break it.", "Gather 20 cobblestone", Items.COBBLESTONE, null,
						t -> t.count(Items.COBBLESTONE) >= 20),
				step("Build stone walls", "Place cobblestone to build walls at least 2 blocks high around a small space. Place 16 blocks.",
						"Leave a gap 1 block wide and 2 tall for your door.", "Place 16 cobblestone as walls", Items.COBBLESTONE, null,
						t -> t.cobblePlaced() >= 16),
				step("Hang your door", "Put your door in the gap in your wall.",
						"RIGHT-CLICK the door to open and close it.", "Place your door", Items.OAK_DOOR, null, Tracker::doorNearby),
				step("Light it up", "Place a torch inside your house.",
						"No torches? Craft them: coal on top of a stick.", "Place a torch inside", Items.TORCH,
						grid(null, Items.COAL, null, null, S, null, null, null, null), Tracker::torchNearby).asRecall()),
			List.of("Add a roof and windows (smelt sand into glass)", "Build a tower you can see from far away", "Decorate with stairs, slabs and flowers"), 8),

		new Role("explorer", "Explorer", 0xFF55FFFF, Items.COMPASS, "See what's out there: new lands, caves, oceans.",
			List.of("Explorers go see what's out there. Every world is different and goes on almost forever: deserts, jungles, snowy mountains, giant caves, ocean ruins, villages, and other dimensions.",
					"Good fit if you love maps, hiking, or wondering what's over the next hill."),
			"Build a boat, travel far, and climb high.",
			List.of(
				step("Make a boat", "At your crafting table: 5 planks in a U shape (bottom row full, plus the two middle-row edges).",
						"Boats are the fastest way to cross water.", "Make a boat", Items.OAK_BOAT,
						grid(null, null, null, P, null, P, P, P, P), Tracker::hasBoat),
				step("Ride your boat", "Find water, place the boat on it (RIGHT-CLICK), then RIGHT-CLICK the boat to get in. Steer with W A S D.",
						"Press Shift to get out.", "Get in your boat on water", Items.OAK_BOAT, null, Tracker::riding),
				step("Travel 300 blocks", "Pick a direction and go, by boat or on foot. Watch how the land changes.",
						"Remember a landmark so you can find your way back.", "Travel 300 blocks", Items.COMPASS, null,
						t -> t.travelled() >= 300),
				step("Climb up high", "Find a mountain or a tall hill and climb to the top, up to height 100.",
						"Press F3 to see your height (the Y number).", "Climb up to height 100", Items.FEATHER, null,
						t -> t.height() >= 100)),
			List.of("Find a village", "Go into a cave and find iron ore", "Build a Nether portal (you'll need obsidian)"), 8),

		new Role("adventurer", "Adventurer", 0xFFFF5555, Items.IRON_SWORD, "Seek challenges: monsters, treasure, bosses.",
			List.of("Adventurers take on challenges. Monsters come out at night and in dark caves, dungeons hide treasure, and the biggest challenge of all is the Ender Dragon.",
					"Good fit if you like action, games with a goal, and getting stronger."),
			"Upgrade to iron and win a fight.",
			List.of(
				step("Make a stone pickaxe", "At your crafting table: 3 cobblestone across the top, 2 sticks down the middle.",
						"You need a stone pickaxe to mine iron.", "Make a stone pickaxe", Items.STONE_PICKAXE,
						grid(C, C, C, null, S, null, null, S, null), t -> t.has(Items.STONE_PICKAXE)).asRecall(),
				step("Mine 2 iron ore", "Look for stone with orange-tan specks, usually in caves and on cliffs. Mine it with your stone pickaxe.",
						"Iron ore drops raw iron.", "Mine 2 raw iron", Items.RAW_IRON, null, t -> t.count(Items.RAW_IRON) + t.count(Items.IRON_INGOT) >= 2),
				step("Make a furnace", "At your crafting table: 8 cobblestone around the edge, middle empty.",
						"You'll use it to turn raw iron into iron.", "Make a furnace", Items.FURNACE,
						grid(C, C, C, C, null, C, C, C, C), t -> t.has(Items.FURNACE) || t.furnaceNearby()).asRecall(),
				step("Smelt your iron", "Place the furnace and open it. Raw iron in the top slot, fuel (coal, logs or planks) in the bottom. Take the iron ingots.",
						"Smelting takes about 10 seconds per item.", "Smelt 2 iron ingots", Items.IRON_INGOT, null, t -> t.count(Items.IRON_INGOT) >= 2),
				step("Make an iron sword", "At your crafting table: 2 iron ingots stacked in the middle column, 1 stick under them.",
						"Iron is a big upgrade from stone.", "Make an iron sword", Items.IRON_SWORD,
						grid(null, I, null, null, I, null, null, S, null), t -> t.has(Items.IRON_SWORD)),
				step("Defeat the Training Zombie", "A Training Zombie appears in front of you when you close this. Hit, step back, hit again.",
						"On Peaceful difficulty zombies can't appear, so switch to Easy in Options.", "Defeat the Training Zombie", Items.ZOMBIE_HEAD, null,
						Tracker::trainingZombieDefeated)),
			List.of("Make a full set of iron armor", "Explore a cave with torches", "Find a dungeon and claim its treasure"), 10),

		new Role("engineer", "Engineer", 0xFFFF55FF, Items.REDSTONE, "Make things that work by themselves.",
			List.of("Engineers make things that work by themselves. Minecraft has its own kind of electricity called redstone, which powers doors, lights, traps and farms that harvest themselves.",
					"Good fit if you like puzzles, figuring out how things work, or building gadgets."),
			"Build your first machine: a door that opens by itself.",
			List.of(
				step("Make a door", "At your crafting table, fill the left two columns with planks: 6 planks.",
						"One recipe gives you 3 doors.", "Make a door", Items.OAK_DOOR, grid(P, P, null, P, P, null, P, P, null), Tracker::hasDoor).asRecall(),
				step("Make a pressure plate", "At your crafting table, put 2 planks side by side.",
						"A pressure plate is a switch you step on.", "Make a pressure plate", Items.OAK_PRESSURE_PLATE,
						grid(null, null, null, P, P, null, null, null, null), Tracker::hasPressurePlate),
				step("Place your door", "Place the door on the ground.",
						"Leave open ground in front of it.", "Place your door", Items.OAK_DOOR, null, Tracker::doorNearby),
				step("Wire it up", "Put the pressure plate on the ground right in front of your door, then step on it.",
						"The plate sends a signal and the door hears it. That's the idea behind every redstone machine.",
						"Put the plate by your door and step on it", Items.REDSTONE, null, Tracker::onPlateByDoor)),
			List.of("Mine deep for redstone dust", "Build a hidden door with pistons", "Build a farm that harvests itself"), 6),

		new Role("homesteader", "Homesteader", 0xFF55FF55, Items.WHEAT, "Grow food, raise animals, build a cozy life.",
			List.of("Homesteaders make a home and a life. Plant crops, raise animals, cook food, trade with villagers, and build a farm that keeps you fed.",
					"Good fit if you like gardening, cooking, animals, or taking care of things."),
			"Plant a farm and fence in a pen.",
			List.of(
				step("Gather seeds", "Break tall grass. Sometimes seeds drop. Collect at least 3.",
						"Seeds grow into wheat, which makes bread and feeds animals.", "Collect 3 seeds", Items.WHEAT_SEEDS, null,
						t -> t.count(Items.WHEAT_SEEDS) >= 3),
				step("Make a hoe", "At your crafting table: 2 planks in the top row (left and middle), then 2 sticks down the middle.",
						"The hoe is a farming tool.", "Make a hoe", Items.WOODEN_HOE, grid(P, P, null, null, S, null, null, S, null), Tracker::hasHoe),
				step("Till and plant", "RIGHT-CLICK dirt with your hoe to make soil, then RIGHT-CLICK the soil with seeds. Plant 3.",
						"Crops grow faster near water and in light.", "Plant 3 seeds", Items.WHEAT, null, t -> t.cropsNearby() >= 3),
				step("Make fences", "At your crafting table: planks, stick, plank across the top two rows (2 sticks and 4 planks make 3 fences).",
						"Fences keep animals in.", "Make 6 fences", Items.OAK_FENCE,
						grid(P, S, P, P, S, P, null, null, null), t -> t.countFences() >= 6),
				step("Build a pen", "Place 6 fences in a ring to make a small pen.",
						"Lead animals in by holding wheat; they follow you.", "Place 6 fences", Items.OAK_FENCE, null,
						t -> t.fencesPlaced() >= 6)),
			List.of("Breed two animals (cows love wheat)", "Bake bread from 3 wheat", "Trade with a villager"), 8)
	);

	private static final int ARROW = 0xFF4FE3FF, NO = 0xFFFF5A6E, YES = 0xFF5CFFA1, WARN = 0xFFFFC547;

	public static final List<Tip> TIPS = List.of(
		new Tip("Shelter before sunset", "Monsters come out at night.", Items.OAK_DOOR,
			List.of(i(Items.CLOCK), g("→", ARROW), i(Items.ZOMBIE_HEAD), g("→", ARROW), i(Items.OAK_DOOR))),
		new Tip("Beds skip the night", "They also set your respawn point.", BED,
			List.of(i(BED), g("Zzz", ARROW), g("→", ARROW), i(Items.CLOCK), g("☀", WARN))),
		new Tip("Pickaxe before sword", "You need it to mine stone.", Items.WOODEN_PICKAXE,
			List.of(i(Items.WOODEN_PICKAXE), g("→", ARROW), i(Items.COBBLESTONE), g("→", ARROW), i(Items.STONE_SWORD))),
		new Tip("Light keeps monsters away", "Torches stop them spawning nearby.", Items.TORCH,
			List.of(i(Items.TORCH), i(Items.TORCH), g("✖", NO), i(Items.ZOMBIE_HEAD))),
		new Tip("Never dig straight down", "You could fall into lava.", Items.LAVA_BUCKET,
			List.of(i(Items.WOODEN_PICKAXE), g("↓", NO), g("?", WARN), g("↓", NO), i(Items.LAVA_BUCKET))),
		new Tip("Hear a hiss? Run!", "A creeper is about to explode.", Items.CREEPER_HEAD,
			List.of(i(Items.CREEPER_HEAD), g("sss", YES), i(Items.TNT), g("←", WARN), g("!", WARN))),
		new Tip("A full belly heals you", "Hearts refill when you're well fed.", Items.COOKED_BEEF,
			List.of(i(Items.COOKED_BEEF), i(Items.BREAD), g("→", ARROW), g("❤", NO), g("❤", NO))),
		new Tip("Mark your way home", "It's easy to get lost.", Items.COMPASS,
			List.of(i(Items.TORCH), g("···", ARROW), i(Items.TORCH), g("···", ARROW), i(Items.COMPASS))),
		new Tip("Chests keep your stuff safe", "You drop everything when you die.", Items.CHEST,
			List.of(i(Items.DIAMOND), i(Items.IRON_INGOT), g("→", ARROW), i(Items.CHEST), g("✔", YES))),
		new Tip("Use the recipe book", "The green book shows what you can craft.", Items.KNOWLEDGE_BOOK,
			List.of(i(Items.KNOWLEDGE_BOOK), g("→", ARROW), i(Items.CRAFTING_TABLE), g("→", ARROW), i(Items.WOODEN_PICKAXE))),
		new Tip("Don't stare at Endermen", "Eye contact makes them attack.", Items.ENDER_PEARL,
			List.of(i(Items.ENDER_EYE), g("✖", NO), i(Items.ENDER_PEARL), g("→", ARROW), g("↓", YES))),
		new Tip("Watch your step", "Falling from high up hurts.", Items.FEATHER,
			List.of(i(Items.FEATHER), g("↓", WARN), g("↓", WARN), g("→", ARROW), g("❤", NO)))
	);

	public static Role role(String id) {
		if (FIRST_DAY.id().equals(id)) return FIRST_DAY;
		for (Role r : ROLES) if (r.id().equals(id)) return r;
		for (Role r : SIDE_LESSONS) if (r.id().equals(id)) return r;
		return null;
	}

	public static boolean isSideLesson(Role r) { return SIDE_LESSONS.contains(r); }
}
