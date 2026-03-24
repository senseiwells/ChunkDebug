package me.senseiwells.chunkdebug;

import me.senseiwells.chunkdebug.common.network.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkDebug implements ModInitializer {
	public static final String MOD_ID = "chunk-debug";
	public static final Logger LOGGER = LogManager.getLogger("ChunkDebug");

	public static final int PROTOCOL_VERSION = 4;

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(HelloPayload.TYPE, HelloPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ByePayload.TYPE, ByePayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ChunkDataPayload.TYPE, ChunkDataPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ChunkUnloadPayload.TYPE, ChunkUnloadPayload.STREAM_CODEC);

		PayloadTypeRegistry.serverboundPlay().register(StartWatchingPayload.TYPE, StartWatchingPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(StopWatchingPayload.TYPE, StopWatchingPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ChunkRefreshPayload.TYPE, ChunkRefreshPayload.STREAM_CODEC);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
