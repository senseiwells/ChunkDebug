package me.senseiwells.chunkdebug;

import me.senseiwells.chunkdebug.common.network.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkDebug implements ModInitializer {
	public static final String MOD_ID = "chunk-debug";
	public static final Logger LOGGER = LogManager.getLogger("ChunkDebug");

	public static final int PROTOCOL_VERSION = 1;

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(HelloPayload.TYPE, HelloPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ByePayload.TYPE, ByePayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ChunkDataPayload.TYPE, ChunkDataPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ChunkUnloadPayload.TYPE, ChunkUnloadPayload.STREAM_CODEC);

		PayloadTypeRegistry.playC2S().register(StartWatchingPayload.TYPE, StartWatchingPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(StopWatchingPayload.TYPE, StopWatchingPayload.STREAM_CODEC);
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
