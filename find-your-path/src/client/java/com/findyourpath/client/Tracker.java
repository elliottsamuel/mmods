package com.findyourpath.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Reads the game to decide whether the current step is done. Also keeps the small per-step counters. */
public final class Tracker {
	public static final String ZOMBIE_NAME = "Training Zombie";

	private LocalPlayer player;
	private Level level;

	// per-step counters (reset when a step starts)
	double sprintDist, travelDist;
	int dirtHigh = -1, dirtPlacedTotal;
	int cobbleHigh = -1, cobblePlacedTotal;
	int fenceHigh = -1, fencePlacedTotal;
	boolean zombieSeen, sawNight;
	private double lastX = Double.NaN, lastZ;

	void bind(LocalPlayer p) {
		this.player = p;
		this.level = p.level();
	}

	void resetStep() {
		sprintDist = 0;
		travelDist = 0;
		dirtHigh = -1;
		dirtPlacedTotal = 0;
		cobbleHigh = -1;
		cobblePlacedTotal = 0;
		fenceHigh = -1;
		fencePlacedTotal = 0;
		zombieSeen = false;
		sawNight = false;
		lastX = Double.NaN;
	}

	/** Called every tick while a quest is running. */
	void tick() {
		double x = player.getX(), z = player.getZ();
		if (!Double.isNaN(lastX)) {
			double d = Math.hypot(x - lastX, z - lastZ);
			if (d < 5) { // ignore teleports
				travelDist += d;
				if (player.isSprinting()) sprintDist += d;
			}
		}
		lastX = x;
		lastZ = z;

		// "placed" = how many have left your inventory since the step began
		int dirt = count(Items.DIRT);
		if (dirtHigh < 0 || dirt > dirtHigh) dirtHigh = dirt;
		else if (dirt < dirtHigh) { dirtPlacedTotal += dirtHigh - dirt; dirtHigh = dirt; }
		int cobble = count(Items.COBBLESTONE);
		if (cobbleHigh < 0 || cobble > cobbleHigh) cobbleHigh = cobble;
		else if (cobble < cobbleHigh) { cobblePlacedTotal += cobbleHigh - cobble; cobbleHigh = cobble; }
		int fences = countFences();
		if (fenceHigh < 0 || fences > fenceHigh) fenceHigh = fences;
		else if (fences < fenceHigh) { fencePlacedTotal += fenceHigh - fences; fenceHigh = fences; }
		if (isNight()) sawNight = true;
	}

	// ---- time of day (26.x keeps time on the overworld "world clock")
	public long timeOfDay() { return Math.floorMod(level.getOverworldClockTime(), 24000L); }
	public boolean isNight() { long t = timeOfDay(); return t >= 12542 && t <= 23460; }
	/** Real-time minutes until night (0 if it's already night). One Minecraft day is 20 real minutes. */
	public int minutesToNight() {
		long t = timeOfDay();
		if (isNight()) return 0;
		long ticks = t < 12542 ? 12542 - t : 24000 - t + 12542;
		return (int) Math.ceil(ticks / 1200.0);
	}
	public boolean underRoof() { return !level.canSeeSky(player.blockPosition().above()); }
	public boolean morningCame() { return sawNight && !isNight(); }

	// ---- movement / position
	public double height() { return player.getY(); }
	public boolean riding() { return player.getVehicle() != null; }

	// ---- inventory
	public int count(Item item) {
		Inventory inv = player.getInventory();
		int n = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.is(item)) n += s.getCount();
		}
		ItemStack held = player.containerMenu.getCarried();   // the item on your mouse cursor counts too
		if (held.is(item)) n += held.getCount();
		return n;
	}

	public int count(TagKey<Item> tag) {
		Inventory inv = player.getInventory();
		int n = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.is(tag)) n += s.getCount();
		}
		ItemStack held = player.containerMenu.getCarried();
		if (held.is(tag)) n += held.getCount();
		return n;
	}

	public boolean has(Item item) { return count(item) > 0; }

	/** Counts an ingredient the way recipes do: any planks for planks, any log for logs. */
	public int countMatching(Item want) {
		if (want == net.minecraft.world.item.Items.OAK_PLANKS) return countPlanks();
		if (want == net.minecraft.world.item.Items.OAK_LOG) return countLogs();
		return count(want);
	}
	public int countLogs() { return count(ItemTags.LOGS); }
	public int countPlanks() { return count(ItemTags.PLANKS); }
	public boolean hasDoor() { return count(ItemTags.WOODEN_DOORS) > 0; }
	public boolean hasPressurePlate() { return count(ItemTags.WOODEN_PRESSURE_PLATES) > 0; }
	public boolean hasHoe() { return count(ItemTags.HOES) > 0; }
	private static final Item[] FLOWERS = {net.minecraft.world.item.Items.POPPY, net.minecraft.world.item.Items.DANDELION,
			net.minecraft.world.item.Items.CORNFLOWER, net.minecraft.world.item.Items.OXEYE_DAISY, net.minecraft.world.item.Items.AZURE_BLUET,
			net.minecraft.world.item.Items.ALLIUM, net.minecraft.world.item.Items.BLUE_ORCHID, net.minecraft.world.item.Items.LILY_OF_THE_VALLEY,
			net.minecraft.world.item.Items.RED_TULIP, net.minecraft.world.item.Items.ORANGE_TULIP, net.minecraft.world.item.Items.WHITE_TULIP,
			net.minecraft.world.item.Items.PINK_TULIP};

	private static String idOf(ItemStack s) { return BuiltInRegistries.ITEM.getKey(s.getItem()).getPath(); }

	private boolean anyMatching(java.util.function.Predicate<ItemStack> test) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) if (test.test(inv.getItem(i))) return true;
		return test.test(player.containerMenu.getCarried());
	}

	private static final java.util.Set<String> FOODS = java.util.Set.of("beef", "porkchop", "chicken", "mutton", "rabbit", "cod", "salmon",
			"cooked_beef", "cooked_porkchop", "cooked_chicken", "cooked_mutton", "cooked_rabbit", "cooked_cod", "cooked_salmon",
			"apple", "bread", "carrot", "potato", "baked_potato", "sweet_berries", "melon_slice", "cookie", "glow_berries");
	private static final java.util.Set<String> COOKED = java.util.Set.of("cooked_beef", "cooked_porkchop", "cooked_chicken",
			"cooked_mutton", "cooked_rabbit", "cooked_cod", "cooked_salmon", "baked_potato");

	public boolean hasFood() { return anyMatching(s -> !s.isEmpty() && FOODS.contains(idOf(s))); }
	public boolean hasCookedFood() { return anyMatching(s -> !s.isEmpty() && COOKED.contains(idOf(s))); }
	public boolean hasBed() { return anyMatching(s -> !s.isEmpty() && (idOf(s).endsWith("_bed") || idOf(s).equals("bed"))); }
	public boolean hasBoat() { return anyMatching(s -> !s.isEmpty() && (idOf(s).endsWith("_boat") || idOf(s).endsWith("_raft"))); }
	public int countWool() { return count(ItemTags.WOOL); }
	public int countFences() { return count(ItemTags.WOODEN_FENCES); }
	public int cobblePlaced() { return cobblePlacedTotal; }
	public int fencesPlaced() { return fencePlacedTotal; }

	public boolean hasFlower() {
		for (Item f : FLOWERS) if (has(f)) return true;
		return false;
	}

	// ---- counters
	public double sprinted() { return sprintDist; }
	public double travelled() { return travelDist; }
	public int dirtPlaced() { return dirtPlacedTotal; }
	public boolean inWater() { return player.isInWater(); }

	// ---- world around the player
	private int countBlocks(int radius, java.util.function.Predicate<BlockState> test) {
		BlockPos c = player.blockPosition();
		int n = 0;
		for (BlockPos p : BlockPos.betweenClosed(c.offset(-radius, -3, -radius), c.offset(radius, 3, radius))) {
			if (test.test(level.getBlockState(p))) n++;
		}
		return n;
	}

	public boolean tableNearby() { return countBlocks(6, s -> s.is(Blocks.CRAFTING_TABLE)) > 0; }
	public boolean chestNearby() { return countBlocks(6, s -> s.is(Blocks.CHEST)) > 0; }
	public boolean furnaceNearby() { return countBlocks(6, s -> s.is(Blocks.FURNACE)) > 0; }
	public boolean torchNearby() { return countBlocks(6, s -> s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH)) > 0; }
	public boolean doorNearby() { return countBlocks(6, s -> s.is(BlockTags.WOODEN_DOORS)) > 0; }
	public int cropsNearby() { return countBlocks(6, s -> s.is(Blocks.WHEAT)); }

	public boolean onPlateByDoor() {
		BlockState here = level.getBlockState(player.blockPosition());
		if (!here.is(BlockTags.WOODEN_PRESSURE_PLATES)) return false;
		BlockPos c = player.blockPosition();
		for (BlockPos p : BlockPos.betweenClosed(c.offset(-2, -1, -2), c.offset(2, 1, 2))) {
			if (level.getBlockState(p).is(BlockTags.WOODEN_DOORS)) return true;
		}
		return false;
	}

	// ---- training zombie
	private Entity findTrainingZombie() {
		AABB box = player.getBoundingBox().inflate(48);
		for (Entity e : level.getEntities((Entity) null, box, e -> e.getType() == EntityTypes.ZOMBIE)) {
			if (e.hasCustomName() && ZOMBIE_NAME.equals(e.getCustomName().getString())) return e;
		}
		return null;
	}

	public boolean trainingZombieDefeated() {
		Entity z = findTrainingZombie();
		if (z != null && z.isAlive()) {
			zombieSeen = true;
			return false;
		}
		return zombieSeen;
	}

	/** Spawns the Training Zombie 5 blocks in front of the player. Only works in singleplayer / on the host of a LAN world. */
	static void spawnTrainingZombie(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null || mc.player == null) return;
		String who = mc.player.getScoreboardName();
		String summon = "execute as " + who + " at @s run summon minecraft:zombie ^ ^ ^5 "
				+ "{CustomName:\"" + ZOMBIE_NAME + "\",CustomNameVisible:1b,PersistenceRequired:1b,Health:8f,Tags:[\"fyp_dummy\"],"
				+ "active_effects:[{id:\"minecraft:fire_resistance\",duration:-1,amplifier:0,show_particles:0b}]}";
		server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), summon));
	}

	static void removeTrainingZombie(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null) return;
		server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
				"kill @e[type=minecraft:zombie,tag=fyp_dummy]"));
	}
}
