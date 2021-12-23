package chunkdebug.feature;

import chunkdebug.ChunkDebugServer;
import chunkdebug.utils.ChunkMapSerializer;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChunkServerNetworkHandler {
	public static Identifier ESSENTIAL_CHANNEL = new Identifier("essentialclient", "chunkdebug");
	public static final int
		HELLO = 0,
		DATA = 16;

	private final Map<ServerPlayerEntity, ServerWorld> validPlayersEnabled = new HashMap<>();
	private final Map<ServerWorld, Map<ChunkPos, ChunkHolder.LevelType>> serverWorldChunks = new HashMap<>();
	private final Set<ServerWorld> worldsNeedUpdating = new HashSet<>();

	public void onHello(ServerPlayerEntity player) {
		this.validPlayersEnabled.put(player, null);
		ChunkDebugServer.LOGGER.info("%s has logged in with ChunkDebug".formatted(player.getEntityName()));
	}

	public void removePlayer(ServerPlayerEntity player) {
		this.validPlayersEnabled.remove(player);
	}

	public void handlePacket(PacketByteBuf packetByteBuf, ServerPlayerEntity player) {
		if (packetByteBuf != null) {
			switch (packetByteBuf.readVarInt()) {
				case HELLO -> this.onHello(player);
				case DATA -> this.processPacket(packetByteBuf, player);
			}
		}
	}

	private void processPacket(PacketByteBuf packetByteBuf, ServerPlayerEntity player) {
		if (this.validPlayersEnabled.containsKey(player)) {
			Identifier identifier = packetByteBuf.readIdentifier();
			this.validPlayersEnabled.replace(player, ChunkDebugServer.server.getWorld(RegistryKey.of(Registry.WORLD_KEY, identifier)));
			this.sendClientUpdate(player, true);
		}
	}

	public void addWorld(ServerWorld world) {
		this.serverWorldChunks.put(world, new HashMap<>());
	}

	public void updateChunkMap(ServerWorld world, ChunkPos pos, ChunkHolder.LevelType levelType) {
		Map<ChunkPos, ChunkHolder.LevelType> chunkMap = this.serverWorldChunks.get(world);
		this.worldsNeedUpdating.add(world);
		if (levelType == ChunkHolder.LevelType.INACCESSIBLE) {
			chunkMap.remove(pos);
			return;
		}
		chunkMap.put(pos, levelType);
	}

	private void sendClientUpdate(ServerPlayerEntity player, boolean force) {
		ServerWorld world = this.validPlayersEnabled.get(player);
		if (world == null || (!this.worldsNeedUpdating.contains(world) && !force)) {
			return;
		}
		Map<ChunkPos, ChunkHolder.LevelType> chunkMap = this.serverWorldChunks.get(world);
		if (chunkMap == null || chunkMap.isEmpty()) {
			return;
		}
		NbtCompound serializedMap = ChunkMapSerializer.serialize(world, chunkMap);
		player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
			ESSENTIAL_CHANNEL,
			new PacketByteBuf(Unpooled.buffer()).writeVarInt(DATA).writeNbt(serializedMap)
		));
	}

	public void tickUpdate() {
		for (ServerPlayerEntity players : this.validPlayersEnabled.keySet()) {
			sendClientUpdate(players, false);
		}
		this.worldsNeedUpdating.clear();
	}
}
