package me.senseiwells.chunkdebug.server;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.senseiwells.chunkdebug.ChunkDebug;
import me.senseiwells.chunkdebug.common.network.*;
import me.senseiwells.chunkdebug.common.utils.ChunkData;
import me.senseiwells.chunkdebug.server.config.ChunkDebugServerConfig;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.Consumer;

public class ChunkDebugServer implements ModInitializer {
	private static final int PACKET_PARTITION_SIZE = 20_000;

	private static ChunkDebugServer instance;

	private final Multimap<ResourceKey<Level>, UUID> watching = HashMultimap.create();
	private final ChunkDebugServerConfig config = ChunkDebugServerConfig.read();

	public static ChunkDebugServer getInstance() {
		return instance;
	}

	@Override
	public void onInitialize() {
		instance = this;

		ServerTickEvents.END_SERVER_TICK.register(this::sendUpdatesToWatching);
		ServerPlayConnectionEvents.JOIN.register(this::sendHelloPayload);

		ServerPlayNetworking.registerGlobalReceiver(StartWatchingPayload.TYPE, this::handleStartWatching);
		ServerPlayNetworking.registerGlobalReceiver(StopWatchingPayload.TYPE, this::handleStopWatching);
	}

	public boolean isPermitted(ServerPlayer player) {
		if (player.server.isDedicatedServer() && this.config.requirePermissions()) {
			return Permissions.check(player, "chunk-debug", 2);
		}
		return true;
	}

	@ApiStatus.Internal
	public void onOpPlayer(ServerPlayer player) {
		player.connection.send(new ClientboundCustomPayloadPacket(HelloPayload.INSTANCE));
	}

	@ApiStatus.Internal
	public void onDeOpPlayer(ServerPlayer player) {
		player.connection.send(new ClientboundCustomPayloadPacket(ByePayload.INSTANCE));
	}

	private void sendHelloPayload(ServerGamePacketListenerImpl connection, PacketSender sender, MinecraftServer server) {
		// We have to do this later in the tick because luckperms uses the same
		// event to load permissions, and we need that information to be loaded
		// before we check whether the player is permitted or not.
		server.tell(new TickTask(server.getTickCount(), () -> {
			if (this.isPermitted(connection.player)) {
				sender.sendPacket(HelloPayload.INSTANCE);
			}
		}));
	}

	private void sendUpdatesToWatching(MinecraftServer server) {
		Iterator<ResourceKey<Level>> dimensions = this.watching.keySet().iterator();
		List<Runnable> tasks = new LinkedList<>();
		while (dimensions.hasNext()) {
			ResourceKey<Level> dimension = dimensions.next();
			ServerLevel level = server.getLevel(dimension);
			if (level == null) {
				dimensions.remove();
				continue;
			}

			List<ServerPlayer> players = new ArrayList<>();
			for (UUID next : this.watching.get(dimension)) {
				ServerPlayer player = server.getPlayerList().getPlayer(next);
				if (player == null) {
					tasks.add(() -> this.watching.remove(dimension, next));
				} else {
					players.add(player);
				}
			}

			ChunkDebugTracker tracker = ((ChunkDebugTrackerHolder) level).chunkdebug$getTracker();
			ChunkDebugTracker.DirtyChunks dirty = tracker.getDirtyChunks();
			this.partitionInto(dirty.updated(), partition -> {
				ChunkDataPayload payload = new ChunkDataPayload(dimension, partition, server.getTickCount(), false);
				ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(payload);
				for (ServerPlayer player : players) {
					player.connection.send(packet);
				}
			});
			if (!dirty.removed().isEmpty()) {
				ChunkUnloadPayload payload = new ChunkUnloadPayload(dimension, dirty.removed().toLongArray());
				ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(payload);
				for (ServerPlayer player : players) {
					player.connection.send(packet);
				}
			}
		}
		tasks.forEach(Runnable::run);
	}

	private void handleStartWatching(StartWatchingPayload payload, ServerPlayNetworking.Context context) {
		ServerPlayer player = context.player();
		if (!this.isPermitted(player)) {
			ChunkDebug.LOGGER.warn("Player {} tried to use chunk-debug without permission!", player.getScoreboardName());
			return;
		}

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
				this.partitionInto(data, partition -> {
					context.responseSender().sendPacket(new ChunkDataPayload(dimension, partition, tickCount, true));
				});
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

	private <T> void partitionInto(
		Collection<T> data,
		Consumer<Collection<T>> consumer
	) {
		if (data.isEmpty()) {
			return;
		}
		if (data.size() < PACKET_PARTITION_SIZE) {
			consumer.accept(data);
			return;
		}
		for (Collection<T> partition : Iterables.partition(data, PACKET_PARTITION_SIZE)) {
			consumer.accept(partition);
		}
	}
}
