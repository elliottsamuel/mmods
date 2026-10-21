package com.findyourpath.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;

import com.findyourpath.client.Quests.Role;
import com.findyourpath.client.Quests.Step;

public class FindYourPathClient implements ClientModInitializer {
	public static final String MOD_ID = "findyourpath";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static KeyMapping openKey;
	private static SaveData save;
	private static final Tracker tracker = new Tracker();
	private static final Random random = new Random();

	// current world + quest
	private static String worldKey;
	private static Role role;
	private static int stepIndex;
	private static boolean zombieSpawnedForStep;

	// timers (ticks)
	private static final int FLASH_TICKS = 36;
	private static int checkTimer, popupTimer = -1, flashTimer, tipTimer, nextTipAt = 1200;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitializeClient() {
		save = SaveData.load();

		openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.findyourpath.open",
				InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KeyMapping.Category.register(id("main"))));

		ClientTickEvents.END_CLIENT_TICK.register(FindYourPathClient::tick);
		HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE, id("hint"), FindYourPathClient::drawHud);

		// helper panel next to the crafting grid
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (screen instanceof InventoryScreen || screen instanceof CraftingScreen) {
				ScreenEvents.afterExtract(screen).register((s, g, mx, my, delta) -> drawCraftingHelper(g, s.width, s.height));
			}
		});
	}

	// ------------------------------------------------------------------ quest state
	public static Role currentRole() { return role; }
	public static int currentStepIndex() { return stepIndex; }
	private static Step currentStep() { return role == null ? null : role.steps().get(stepIndex); }

	static void start(Role r) {
		Minecraft mc = Minecraft.getInstance();
		if (role != null) Tracker.removeTrainingZombie(mc);
		role = r;
		stepIndex = 0;
		beginStep();
		persist();
	}

	static void abandon() {
		Tracker.removeTrainingZombie(Minecraft.getInstance());
		role = null;
		stepIndex = 0;
		popupTimer = -1;
		persist();
	}

	private static void beginStep() {
		tracker.resetStep();
		zombieSpawnedForStep = false;
	}

	/** After the player dismisses a step tablet. Spawns the Training Zombie when that's the step. */
	static void onStepScreenClosed() {
		Step st = currentStep();
		Minecraft mc = Minecraft.getInstance();
		if (st != null && st.title().startsWith("Defeat the Training Zombie") && !zombieSpawnedForStep
				&& mc.level != null && mc.level.getDifficulty() != Difficulty.PEACEFUL) {
			Tracker.spawnTrainingZombie(mc);
			zombieSpawnedForStep = true;
		}
	}

	private static void advance() {
		Minecraft mc = Minecraft.getInstance();
		stepIndex++;
		flashTimer = FLASH_TICKS;
		if (stepIndex >= role.steps().size()) {
			Role done = role;
			role = null;
			stepIndex = 0;
			persist();
			mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f));
			Flow.complete(done);
			return;
		}
		mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f));
		beginStep();
		persist();
		popupTimer = FLASH_TICKS - 4; // show the next step once the check is carved
	}

	private static void persist() {
		if (worldKey == null) return;
		if (role == null) save.worlds.remove(worldKey);
		else {
			SaveData.WorldProgress p = new SaveData.WorldProgress();
			p.role = role.id();
			p.step = stepIndex;
			save.worlds.put(worldKey, p);
		}
		save.save();
	}

	private static String computeWorldKey(Minecraft mc) {
		try {
			var server = mc.getSingleplayerServer();
			if (server != null) return "sp:" + server.getWorldData().getLevelName();
			if (mc.getCurrentServer() != null) return "mp:" + mc.getCurrentServer().ip;
		} catch (Exception ignored) {
		}
		return "default";
	}

	// ------------------------------------------------------------------ tick
	private static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			worldKey = null;
			return;
		}
		if (worldKey == null) { // just joined a world: load its progress
			worldKey = computeWorldKey(mc);
			SaveData.WorldProgress p = save.worlds.get(worldKey);
			role = p == null ? null : Quests.role(p.role);
			stepIndex = role == null ? 0 : Math.min(p.step, role.steps().size() - 1);
			beginStep();
			popupTimer = -1;
		}
		tracker.bind(mc.player);

		while (openKey.consumeClick()) {
			if (mc.gui.screen() == null) Flow.welcome();
		}

		if (flashTimer > 0) flashTimer--;

		if (role != null) {
			tracker.tick();
			if (popupTimer > 0 && --popupTimer == 0) {
				popupTimer = -1;
				if (mc.gui.screen() == null) Flow.currentStep();
			}
			if (popupTimer < 0 && mc.gui.screen() == null && ++checkTimer >= 5) {
				checkTimer = 0;
				Step st = currentStep();
				if (st != null && st.check().test(tracker)) advance();
			}
		}

		// random tips, only while actually playing
		if (mc.gui.screen() == null && ++tipTimer >= nextTipAt) {
			tipTimer = 0;
			nextTipAt = 2400 + random.nextInt(3600);
			showRandomTip(mc);
		}
	}

	private static void showRandomTip(Minecraft mc) {
		List<Quests.Tip> unseen = new ArrayList<>();
		for (Quests.Tip t : Quests.TIPS) if (!save.seenTips.contains(t.title())) unseen.add(t);
		if (unseen.isEmpty()) return;
		Quests.Tip tip = unseen.get(random.nextInt(unseen.size()));
		save.seenTips.add(tip.title());
		save.save();
		mc.gui.toastManager().addToast(new TipToast(tip));
	}

	// ------------------------------------------------------------------ HUD line above the hotbar
	private static void drawHud(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		Font font = mc.font;
		int w = g.guiWidth(), h = g.guiHeight();

		if (flashTimer > 0) {
			String done = "✔ Done!";
			g.centeredText(font, done, w / 2, h / 2 - 30, Ui.GREEN);
		}

		String line;
		Step st = currentStep();
		if (st != null) {
			line = role.name() + ", Step " + (stepIndex + 1) + " of " + role.steps().size() + ": " + st.bar() + progressSuffix(st);
		} else {
			line = "Press " + openKey.getTranslatedKeyMessage().getString() + " to open Find Your Path";
		}
		int color = st != null ? role.color() : Ui.GOLD;
		int tw = font.width(line);
		int x = (w - tw) / 2, y = h - 72;
		g.fill(x - 3, y - 2, x + tw + 3, y + 10, 0x66000000);
		g.text(font, line, x, y, color, true);
	}

	private static String progressSuffix(Step st) {
		if (st.title().equals("Travel 200 blocks")) return "  (" + Math.min(200, (int) tracker.travelled()) + "/200)";
		if (st.title().equals("Dig some dirt")) return "  (" + Math.min(10, tracker.count(net.minecraft.world.item.Items.DIRT)) + "/10)";
		if (st.title().equals("Build walls")) return "  (" + Math.min(8, tracker.dirtPlaced()) + "/8)";
		if (st.title().equals("Get 3 logs")) return "  (" + Math.min(3, tracker.countLogs()) + "/3)";
		return "";
	}

	// ------------------------------------------------------------------ helper beside the crafting grid
	private static void drawCraftingHelper(GuiGraphicsExtractor g, int screenW, int screenH) {
		Step st = currentStep();
		if (st == null) return;
		Font font = Minecraft.getInstance().font;
		int pw = 126, x = 6, y = 6, pad = 12;
		List<FormattedCharSequence> title = Ui.wrap(font, st.title(), pw - pad * 2);
		List<FormattedCharSequence> how = st.recipe() == null ? Ui.wrap(font, st.howTo(), pw - pad * 2) : List.of();
		int ph = pad + 11 + title.size() * 10 + 4 + (st.recipe() != null ? Ui.recipeH(st.recipe()) + 6 : how.size() * 10) + pad;
		Ui.smallTablet(g, x, y, pw, ph);
		int cy = y + pad;
		Ui.carved(g, font, role.name() + " " + (stepIndex + 1) + "/" + role.steps().size(), x + pad, cy, Ui.ink(role.color()));
		cy += 11;
		for (FormattedCharSequence l : title) { Ui.carved(g, font, l, x + pad, cy, Ui.INK); cy += 10; }
		cy += 4;
		if (st.recipe() != null) {
			Ui.recipe(g, font, x + (pw - Ui.recipeW(st.recipe())) / 2, cy, st.recipe(), st.icon());
		} else {
			for (FormattedCharSequence l : how) { Ui.carved(g, font, l, x + pad, cy, Ui.INK_SOFT); cy += 10; }
		}
	}
}
