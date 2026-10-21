package com.findyourpath.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
	boolean zombieSeen;
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
		zombieSeen = false;
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

		int dirt = count(net.minecraft.world.item.Items.DIRT);
		if (dirtHigh < 0 || dirt > dirtHigh) dirtHigh = dirt;
		else if (dirt < dirtHigh) {
			dirtPlacedTotal += dirtHigh - dirt;
			dirtHigh = dirt;
		}
	}

	// ---- inventory
	public int count(Item item) {
		Inventory inv = player.getInventory();
		int n = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.is(item)) n += s.getCount();
		}
		return n;
	}

	public int count(TagKey<Item> tag) {
		Inventory inv = player.getInventory();
		int n = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.is(tag)) n += s.getCount();
		}
		return n;
	}

	public boolean has(Item item) { return count(item) > 0; }
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
