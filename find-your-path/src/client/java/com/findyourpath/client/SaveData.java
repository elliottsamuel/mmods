package com.findyourpath.client;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/** Remembers each world's quest progress and which tips you've seen, in config/findyourpath.json. */
public final class SaveData {
	public static final class WorldProgress {
		public String role;
		public int step;
	}

	public Map<String, WorldProgress> worlds = new HashMap<>();
	public Set<String> seenTips = new HashSet<>();

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("findyourpath.json"); }

	public static SaveData load() {
		try (Reader r = Files.newBufferedReader(file())) {
			SaveData d = GSON.fromJson(r, SaveData.class);
			if (d != null) {
				if (d.worlds == null) d.worlds = new HashMap<>();
				if (d.seenTips == null) d.seenTips = new HashSet<>();
				return d;
			}
		} catch (Exception ignored) {
		}
		return new SaveData();
	}

	public void save() {
		try {
			Files.createDirectories(file().getParent());
			try (Writer w = Files.newBufferedWriter(file())) {
				GSON.toJson(this, w);
			}
		} catch (Exception e) {
			FindYourPathClient.LOGGER.warn("Couldn't save Find Your Path progress", e);
		}
	}
}
