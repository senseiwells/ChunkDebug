package me.senseiwells.chunkdebug.server;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import me.senseiwells.chunkdebug.ChunkDebug;
import me.senseiwells.chunkdebug.common.network.ChunkDataPayload;
import me.senseiwells.chunkdebug.common.network.HelloPayload;
import me.senseiwells.chunkdebug.common.network.StartWatchingPayload;
import me.senseiwells.chunkdebug.common.network.StopWatchingPayload;
import me.senseiwells.chunkdebug.common.utils.ChunkData;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.Consumer;

public class ChunkDebugServer implements ModInitializer {
	private static final int PACKET_PARTITION_SIZE = 20_000;

	private final Multimap<ResourceKey<Level>, UUID> watching = HashMultimap.create();

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(this::sendUpdatesToWatching);
		ServerConfigurationConnectionEvents.CONFIGURE.register(this::sendHelloPayload);

		ServerPlayNetworking.registerGlobalReceiver(StartWatchingPayload.TYPE, this::handleStartWatching);
		ServerPlayNetworking.registerGlobalReceiver(StopWatchingPayload.TYPE, this::handleStopWatching);
	}

	private void sendHelloPayload(ServerConfigurationPacketListenerImpl handler, MinecraftServer server) {
		handler.send(new ClientboundCustomPayloadPacket(new HelloPayload(ChunkDebug.PROTOCOL_VERSION)));
	}

	private void sendUpdatesToWatching(MinecraftServer server) {
		Iterator<ResourceKey<Level>> dimensions = this.watching.keySet().iterator();
		while (dimensions.hasNext()) {
			ResourceKey<Level> dimension = dimensions.next();
			ServerLevel level = server.getLevel(dimension);
			if (level == null) {
				dimensions.remove();
				continue;
			}

			List<ServerPlayer> players = new ArrayList<>();
			Iterator<UUID> uuids = this.watching.get(dimension).iterator();
			while (uuids.hasNext()) {
				ServerPlayer player = server.getPlayerList().getPlayer(uuids.next());
				if (player == null) {
					uuids.remove();
				} else {
					players.add(player);
				}
			}

			ChunkDebugTracker tracker = ((ChunkDebugTrackerHolder) level).chunkdebug$getTracker();
			Collection<ChunkData> data = tracker.getChunkUpdates();
			this.partitionIntoPayloads(dimension, data, server.getTickCount(), false, payload -> {
				ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(payload);
				for (ServerPlayer player : players) {
					player.connection.send(packet);
				}
			});
		}
	}

	private void handleStartWatching(StartWatchingPayload payload, ServerPlayNetworking.Context context) {
		ServerPlayer player = context.player();
		MinecraftServer server = context.server();
		int tickCount = server.getTickCount();
		for (ResourceKey<Level> dimension : payload.dimensions()) {
			ServerLevel level = server.getLevel(dimension);
			if (level == null) {
				ChunkDebug.LOGGER.warn("Player {} requested invalid dimension {}", player.getScoreboardName(), dimension);
				return;
			}

			if (this.watching.put(dimension, player.getUUID())) {
				Collection<ChunkData> data = ((ChunkDebugTrackerHolder) level).chunkdebug$getTracker().getChunks();
				this.partitionIntoPayloads(dimension, data, tickCount, true, context.responseSender()::sendPacket);
			}
		}
	}

	private void handleStopWatching(StopWatchingPayload payload, ServerPlayNetworking.Context context) {
		UUID uuid = context.player().getUUID();
		if (payload.dimensions().isEmpty()) {
			for (ResourceKey<Level> dimension : this.watching.keySet()) {
				this.watching.remove(dimension, uuid);
			}
			return;
		}
		for (ResourceKey<Level> dimension : payload.dimensions()) {
			this.watching.remove(dimension, uuid);
		}
	}

	private void partitionIntoPayloads(
		ResourceKey<Level> dimension,
		Collection<ChunkData> data,
		int tick,
		boolean initial,
		Consumer<ChunkDataPayload> consumer
	) {
		if (data.isEmpty()) {
			return;
		}
		if (data.size() < PACKET_PARTITION_SIZE) {
			consumer.accept(new ChunkDataPayload(dimension, data, tick, initial));
			return;
		}
		for (Collection<ChunkData> partition : Iterables.partition(data, PACKET_PARTITION_SIZE)) {
			consumer.accept(new ChunkDataPayload(dimension, partition, tick, initial));
		}
	}
}
