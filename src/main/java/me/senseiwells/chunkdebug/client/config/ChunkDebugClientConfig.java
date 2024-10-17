package me.senseiwells.chunkdebug.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.senseiwells.chunkdebug.ChunkDebug;
import me.senseiwells.chunkdebug.client.utils.Corner;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ChunkDebugClientConfig {
	public Corner minimapCorner;
	public double minimapOffsetX;
	public double minimapOffsetY;
	public int minimapSize;
	public boolean showStages;
	public boolean showTickets;
	public boolean showMinimap;
	public int chunkRetention;

	public ChunkDebugClientConfig(
		Corner minimapCorner,
		double minimapOffsetX,
		double minimapOffsetY,
		int minimapSize,
		boolean showStages,
		boolean showTickets,
		boolean showMinimap,
		int chunkRetention
	) {
		this.minimapCorner = minimapCorner;
		this.minimapOffsetX = minimapOffsetX;
		this.minimapOffsetY = minimapOffsetY;
		this.minimapSize = minimapSize;
		this.showStages = showStages;
		this.showTickets = showTickets;
		this.showMinimap = showMinimap;
		this.chunkRetention = chunkRetention;
	}

	public ChunkDebugClientConfig() {
		this(
			Corner.TOP_RIGHT,
			0.0,
			0.0,
			100,
			true,
			true,
			true,
			0
		);
	}

	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("chunk-debug-client.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	public static final Codec<ChunkDebugClientConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Corner.CODEC.fieldOf("minimap_corner").forGetter(config -> config.minimapCorner),
			Codec.DOUBLE.fieldOf("minimap_offset_x").forGetter(config -> config.minimapOffsetX),
			Codec.DOUBLE.fieldOf("minimap_offset_y").forGetter(config -> config.minimapOffsetY),
			Codec.INT.fieldOf("minimap_size").forGetter(config -> config.minimapSize),
			Codec.BOOL.fieldOf("show_stages").forGetter(config -> config.showStages),
			Codec.BOOL.fieldOf("show_tickets").forGetter(config -> config.showTickets),
			Codec.BOOL.fieldOf("show_minimap").forGetter(config -> config.showMinimap),
			Codec.INT.fieldOf("chunk_retention").forGetter(config -> config.chunkRetention)
		).apply(instance, ChunkDebugClientConfig::new);
	});

	public static ChunkDebugClientConfig read() {
		if (!Files.exists(PATH)) {
			ChunkDebug.LOGGER.info("Generating default client config");
			return write(new ChunkDebugClientConfig());
		}
		try (BufferedReader reader = Files.newBufferedReader(PATH)) {
			JsonElement element = GSON.fromJson(reader, JsonElement.class);
			Optional<ChunkDebugClientConfig> result = CODEC.parse(JsonOps.INSTANCE, element).resultOrPartial();
			if (result.isPresent()) {
				return result.get();
			}
		} catch (IOException e) {
			ChunkDebug.LOGGER.error("Failed to read chunk-debug-client config", e);
		}
		return write(new ChunkDebugClientConfig());
	}

	public static ChunkDebugClientConfig write(ChunkDebugClientConfig config) {
		try {
			Files.createDirectories(PATH.getParent());
			Optional<JsonElement> result = CODEC.encodeStart(JsonOps.INSTANCE, config).resultOrPartial();
			if (result.isPresent()) {
				try (BufferedWriter writer = Files.newBufferedWriter(PATH)) {
					GSON.toJson(result.get(), writer);
				}
			}
		} catch (IOException e) {
			ChunkDebug.LOGGER.error("Failed to write chunk-debug-client config", e);
		}
		return config;
	}
}
