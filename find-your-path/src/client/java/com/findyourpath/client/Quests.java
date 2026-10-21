package com.findyourpath.client;

import java.util.List;
import java.util.function.Predicate;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** All the content: the five paths, their steps, and the tips. */
public final class Quests {
	private Quests() {}

	/** A step: what to do, how to do it, a tip, the short line above the hotbar, an icon, an optional recipe, and how we know it's done. */
	public record Step(String title, String howTo, String tip, String bar, Item icon, Item[] recipe, Check check) {}

	public interface Check extends Predicate<Tracker> {}

	public record Role(String id, String name, int color, Item icon, String tagline, List<String> about,
			String goal, List<Step> steps, List<String> next) {}

	public record Tip(String title, String text, Item icon) {}

	// --- recipe helpers (3x3 grid, read left-to-right, top-to-bottom; null = empty)
	private static final Item P = Items.OAK_PLANKS, S = Items.STICK, C = Items.COBBLESTONE, L = Items.OAK_LOG;
	private static Item[] grid(Item... nine) { return nine; }

	// --- shared steps
	static final Step LOGS = new Step("Get 3 logs",
			"Walk up to a tree. Hold LEFT-CLICK on the trunk until a block breaks off, then walk over the little floating log to pick it up.",
			"It takes a few seconds with bare hands. Keep holding! Collect 3 logs.",
			"Find a tree and get 3 pieces of wood", Items.OAK_LOG, null, t -> t.countLogs() >= 3);
	static final Step PLANKS = new Step("Turn logs into planks",
			"Press E to open your inventory. Drag a log into the small 2x2 crafting grid. Planks appear on the right. Click them to take them.",
			"One log makes 4 planks. Planks are what you'll build most tools with.",
			"Press E and turn your logs into planks", Items.OAK_PLANKS,
			grid(null, null, null, null, L, null, null, null, null), t -> t.countPlanks() >= 1);
	static final Step TABLE = new Step("Make a crafting table",
			"Press E again. Put a plank in all 4 squares of the small grid. Take the crafting table that appears.",
			"The crafting table is your workbench. Almost everything is made on it.",
			"Turn 4 planks into a crafting table", Items.CRAFTING_TABLE,
			grid(P, P, null, P, P, null, null, null, null), t -> t.has(Items.CRAFTING_TABLE) || t.tableNearby());
	static final Step PLACE = new Step("Place your crafting table",
			"Close your inventory. Pick the table in your hotbar (number keys 1-9, or scroll). Look at the ground and RIGHT-CLICK.",
			"Then RIGHT-CLICK the table to open its bigger 3x3 grid. You'll use it next.",
			"Place your crafting table on the ground", Items.CRAFTING_TABLE, null, Tracker::tableNearby);
	static final Step STICKS = new Step("Make sticks",
			"Open your crafting table. Put one plank directly on top of another. You get 4 sticks.",
			"Sticks are the handles for tools and swords.",
			"Make sticks from 2 planks", Items.STICK,
			grid(null, P, null, null, P, null, null, null, null), t -> t.has(Items.STICK));
	static final Step DOOR = new Step("Make a wooden door",
			"Open your crafting table. Fill the left two columns with planks: 6 planks total.",
			"One recipe gives you 3 doors.",
			"Make a door from 6 planks", Items.OAK_DOOR,
			grid(P, P, null, P, P, null, P, P, null), Tracker::hasDoor);

	static Step placeDoor(String tip) {
		return new Step("Place your door",
				"Pick the door in your hotbar, look at the ground and RIGHT-CLICK. A door is 2 blocks tall.",
				tip, "Place your door", Items.OAK_DOOR, null, Tracker::doorNearby);
	}

	public static final List<Role> ROLES = List.of(
		new Role("builder", "Builder", 0xFFFFAA00, Items.BRICKS, "Make places: homes, towers, secret bases.",
			List.of("Builders make places. A cozy cabin, a castle on a cliff, a secret base under a lake. Almost every block in the world can be picked up and placed somewhere else, so the whole world is your building material.",
					"Good fit if you like Legos, drawing, or designing spaces."),
			"Build a tiny hideout with a real door.",
			List.of(LOGS, PLANKS, TABLE, PLACE, DOOR,
				new Step("Dig some dirt", "Look at the ground and hold LEFT-CLICK to dig. Collect 10 blocks of dirt.",
						"Dirt is the easiest building block. It's everywhere.", "Dig 10 blocks of dirt", Items.DIRT, null,
						t -> t.count(Items.DIRT) >= 10),
				new Step("Build walls", "Pick dirt in your hotbar and RIGHT-CLICK to place blocks. Make a small square of walls, 2 blocks high. Place at least 8.",
						"Leave a gap 1 block wide and 2 tall for your door.", "Place 8 dirt blocks to build walls", Items.DIRT, null,
						t -> t.dirtPlaced() >= 8),
				placeDoor("Put it in the gap in your wall. RIGHT-CLICK the door to open and close it. Welcome home!")),
			List.of("Build a two-story house with windows (smelt sand into glass)", "Build a tower you can see from far away", "Decorate with lanterns, flowers and stairs")),

		new Role("explorer", "Explorer", 0xFF55FFFF, Items.COMPASS, "See what's out there: new lands, caves, oceans.",
			List.of("Explorers go see what's out there. Every Minecraft world is different and goes on almost forever: deserts, jungles, snowy mountains, giant caves, ocean ruins, villages, and eventually other dimensions.",
					"Good fit if you love maps, hiking, or wondering what's over the next hill."),
			"Learn to travel fast, cover ground, and bring back a souvenir.",
			List.of(
				new Step("Learn to sprint", "Hold W to walk. Now double-tap W and keep holding it (or hold Ctrl + W) to sprint. Sprint about 10 blocks.",
						"Sprinting makes you hungry. If the drumsticks run low, eat something.", "Double-tap W and sprint 10 blocks", Items.LEATHER_BOOTS, null,
						t -> t.sprinted() >= 10),
				new Step("Travel 200 blocks", "Pick a direction and go. Watch how the land changes as you go.",
						"Press SPACE to jump up hills. Remember a landmark so you can find your way back.", "Travel 200 blocks", Items.COMPASS, null,
						t -> t.travelled() >= 200),
				new Step("Find water and jump in", "Look for blue: a river, lake or ocean. Walk right in.",
						"Hold SPACE to swim up.", "Find water and jump in", Items.WATER_BUCKET, null, Tracker::inWater),
				new Step("Pick a flower", "Find any flower and hold LEFT-CLICK on it. Pick it up as a souvenir of your trip.",
						"Every land type has its own flowers and plants.", "Pick a flower to keep", Items.POPPY, null, Tracker::hasFlower)),
			List.of("Find a village", "Go into a cave and find iron ore", "Build a Nether portal (you'll need obsidian)")),

		new Role("adventurer", "Adventurer", 0xFFFF5555, Items.IRON_SWORD, "Seek challenges: monsters, treasure, bosses.",
			List.of("Adventurers take on challenges. Monsters come out at night and in dark caves. Dungeons hide treasure, and the biggest challenge of all is the Ender Dragon. Adventurers gear up with better tools, weapons and armor.",
					"Good fit if you like action, games with a goal, and getting stronger."),
			"Gear up the smart way: pickaxe first, then a stone sword, then face a Training Zombie.",
			List.of(LOGS, PLANKS, TABLE, PLACE, STICKS,
				new Step("Make a wooden pickaxe", "Open your crafting table. Top row: 3 planks. Then 2 sticks straight down the middle.",
						"Pickaxe first! It lets you dig stone, and stone makes much stronger tools.", "Make a wooden pickaxe", Items.WOODEN_PICKAXE,
						grid(P, P, P, null, S, null, null, S, null), t -> t.has(Items.WOODEN_PICKAXE)),
				new Step("Mine stone", "Find gray stone, often on hillsides or just under the dirt. Hold your pickaxe and hold LEFT-CLICK on it. Collect 2 cobblestone.",
						"Without a pickaxe, stone drops nothing. That's why you made one.", "Mine 2 stone with your pickaxe", Items.COBBLESTONE, null,
						t -> t.count(Items.COBBLESTONE) >= 2),
				new Step("Make a stone sword", "At your crafting table: 2 cobblestone stacked in the middle column, 1 stick under them.",
						"Stone tools are much stronger than wood.", "Make a stone sword", Items.STONE_SWORD,
						grid(null, C, null, null, C, null, null, S, null), t -> t.has(Items.STONE_SWORD)),
				new Step("Defeat the Training Zombie", "A Training Zombie appears in front of you when you close this. Hold your sword and LEFT-CLICK to swing. Hit, step back, hit again.",
						"Your hearts are your health. On Peaceful difficulty zombies can't appear, so switch to Easy in Options.", "Defeat the Training Zombie", Items.ZOMBIE_HEAD, null,
						Tracker::trainingZombieDefeated)),
			List.of("Survive your first night", "Make a full set of iron armor", "Find a dungeon and claim its treasure")),

		new Role("engineer", "Engineer", 0xFFFF55FF, Items.REDSTONE, "Make things that work by themselves.",
			List.of("Engineers make things that work by themselves. Minecraft has its own kind of electricity called redstone, which powers doors, lights, traps and farms that harvest themselves. Some players build working calculators.",
					"Good fit if you like puzzles, figuring out how things work, or building gadgets."),
			"Build your first machine: a door that opens by itself.",
			List.of(LOGS, PLANKS, TABLE, PLACE, DOOR,
				new Step("Make a pressure plate", "At your crafting table, put 2 planks side by side.",
						"A pressure plate is a switch you step on.", "Make a pressure plate from 2 planks", Items.OAK_PRESSURE_PLATE,
						grid(null, null, null, P, P, null, null, null, null), Tracker::hasPressurePlate),
				placeDoor("Leave some open ground in front of it for the next step."),
				new Step("Wire it up", "Place the pressure plate on the ground right in front of your door. Then step on it and watch.",
						"When you step on the plate it sends a signal, and the door hears it. That's the idea behind every redstone machine.",
						"Put the plate by your door and step on it", Items.REDSTONE, null, Tracker::onPlateByDoor)),
			List.of("Mine deep for redstone dust", "Build a hidden door with pistons", "Build a farm that harvests itself")),

		new Role("homesteader", "Homesteader", 0xFF55FF55, Items.WHEAT, "Grow food, raise animals, build a cozy life.",
			List.of("Homesteaders make a home and a life. Plant crops, raise animals, cook food, trade with villagers, and build a farm that keeps you fed. It's the calm, cozy way to play.",
					"Good fit if you like gardening, cooking, animals, or taking care of things."),
			"Plant your first little farm.",
			List.of(
				new Step("Gather seeds", "Find tall grass and hold LEFT-CLICK on it. Sometimes seeds drop. Keep going until you have some.",
						"Seeds grow into wheat, and wheat makes bread and feeds animals.", "Break tall grass to find seeds", Items.WHEAT_SEEDS, null,
						t -> t.has(Items.WHEAT_SEEDS)),
				LOGS, PLANKS, TABLE, PLACE, STICKS,
				new Step("Make a hoe", "At your crafting table: 2 planks in the top row (left and middle), then 2 sticks down the middle.",
						"The hoe is a farming tool.", "Make a hoe", Items.WOODEN_HOE,
						grid(P, P, null, null, S, null, null, S, null), Tracker::hasHoe),
				new Step("Till and plant", "Hold the hoe and RIGHT-CLICK dirt to turn it into soil. Then pick seeds and RIGHT-CLICK the soil. Plant 3.",
						"Crops grow faster near water and in light.", "Till dirt with your hoe and plant 3 seeds", Items.WHEAT, null,
						t -> t.cropsNearby() >= 3)),
			List.of("Breed two animals (cows love wheat)", "Build a fenced pen for your animals", "Trade with a villager"))
	);

	public static final List<Tip> TIPS = List.of(
		new Tip("Shelter before sunset", "Monsters come out at night.", Items.OAK_DOOR),
		new Tip("Beds skip the night", "They also set your respawn point.", Items.CLOCK),
		new Tip("Pickaxe before sword", "You need it to mine stone.", Items.WOODEN_PICKAXE),
		new Tip("Light keeps monsters away", "Torches stop them spawning nearby.", Items.TORCH),
		new Tip("Never dig straight down", "You could fall into lava.", Items.LAVA_BUCKET),
		new Tip("Hear a hiss? Run!", "A creeper is about to explode.", Items.CREEPER_HEAD),
		new Tip("A full belly heals you", "Hearts refill when you're well fed.", Items.COOKED_BEEF),
		new Tip("Mark your way home", "It's easy to get lost.", Items.COMPASS),
		new Tip("Chests keep your stuff safe", "You drop everything when you die.", Items.CHEST),
		new Tip("Use the recipe book", "The green book shows what you can craft.", Items.KNOWLEDGE_BOOK),
		new Tip("Don't stare at Endermen", "Eye contact makes them attack.", Items.ENDER_PEARL),
		new Tip("Watch your step", "Falling from high up hurts.", Items.FEATHER)
	);

	public static Role role(String id) {
		for (Role r : ROLES) if (r.id().equals(id)) return r;
		return null;
	}
}
