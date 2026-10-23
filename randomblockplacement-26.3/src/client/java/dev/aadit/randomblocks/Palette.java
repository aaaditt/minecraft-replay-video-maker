package dev.aadit.randomblocks;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** The player's block palette and placement settings, saved to config/randomblocks-palette.json. */
public final class Palette {
	public enum Mode { RANDOM, GRADIENT }

	public enum Axis { AUTO, X, Y, Z }

	public static final class Entry {
		public String item;
		public int weight = 50;
		public boolean enabled;

		public Entry(String item) {
			this.item = item;
		}

		public Item resolve() {
			Identifier id = Identifier.tryParse(item);
			return id == null ? null : BuiltInRegistries.ITEM.getValue(id);
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("randomblocks-palette.json");
	private static Palette instance;

	/** Ordered list; the order is the gradient order. */
	public List<Entry> entries = new ArrayList<>();
	public Mode mode = Mode.RANDOM;
	public Axis axis = Axis.AUTO;
	public int gradientLength = 16;
	/** Fraction of the gradient length over which neighbouring bands dither, 0..1. */
	public double blend = 0.25;

	public transient boolean active;
	public transient BlockPos anchor;
	public transient Direction anchorDirection;

	public static Palette get() {
		if (instance == null) instance = load();
		return instance;
	}

	public List<Entry> enabledEntries() {
		List<Entry> out = new ArrayList<>();
		for (Entry e : entries) {
			if (e.enabled && e.weight > 0 && e.resolve() != null) out.add(e);
		}
		return out;
	}

	public Entry find(String item) {
		for (Entry e : entries) {
			if (e.item.equals(item)) return e;
		}
		return null;
	}

	public void resetAnchor() {
		anchor = null;
		anchorDirection = null;
	}

	public void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer w = Files.newBufferedWriter(FILE)) {
				GSON.toJson(this, w);
			}
		} catch (IOException e) {
			RandomBlocksClient.LOGGER.error("Could not save palette", e);
		}
	}

	private static Palette load() {
		if (Files.exists(FILE)) {
			try (Reader r = Files.newBufferedReader(FILE)) {
				Palette p = GSON.fromJson(r, Palette.class);
				if (p != null) {
					if (p.entries == null) p.entries = new ArrayList<>();
					if (p.mode == null) p.mode = Mode.RANDOM;
					if (p.axis == null) p.axis = Axis.AUTO;
					return p;
				}
			} catch (Exception e) {
				RandomBlocksClient.LOGGER.error("Could not read palette, starting fresh", e);
			}
		}
		return new Palette();
	}
}
