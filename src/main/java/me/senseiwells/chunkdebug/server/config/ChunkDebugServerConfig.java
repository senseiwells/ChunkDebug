package me.senseiwells.chunkdebug.server.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.senseiwells.chunkdebug.ChunkDebug;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public record ChunkDebugServerConfig(
	boolean requirePermissions
) {
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("chunk-debug-server.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	public static final Codec<ChunkDebugServerConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.BOOL.fieldOf("require_permissions").forGetter(ChunkDebugServerConfig::requirePermissions)
		).apply(instance, ChunkDebugServerConfig::new);
	});

	public static ChunkDebugServerConfig read() {
		if (!Files.exists(PATH)) {
			ChunkDebug.LOGGER.info("Generating default config");
			return write(new ChunkDebugServerConfig(true));
		}
		try (BufferedReader reader = Files.newBufferedReader(PATH)) {
			JsonElement element = GSON.fromJson(reader, JsonElement.class);
			Optional<ChunkDebugServerConfig> result = CODEC.parse(JsonOps.INSTANCE, element).resultOrPartial();
			if (result.isPresent()) {
				return result.get();
			}
		} catch (IOException e) {
			ChunkDebug.LOGGER.error("Failed to read chunk-debug-server config", e);
		}
		return write(new ChunkDebugServerConfig(true));
	}

	private static ChunkDebugServerConfig write(ChunkDebugServerConfig config) {
		try {
			Files.createDirectories(PATH.getParent());
			Optional<JsonElement> result = CODEC.encodeStart(JsonOps.INSTANCE, config).resultOrPartial();
			if (result.isPresent()) {
				try (BufferedWriter writer = Files.newBufferedWriter(PATH)) {
					GSON.toJson(result.get(), writer);
				}
			}
		} catch (IOException e) {
			ChunkDebug.LOGGER.error("Failed to write chunk-debug-server config", e);
		}
		return config;
	}
}
