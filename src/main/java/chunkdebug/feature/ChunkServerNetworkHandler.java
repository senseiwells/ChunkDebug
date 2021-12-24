package chunkdebug.feature;

import chunkdebug.ChunkDebugServer;
import chunkdebug.mixins.ThreadedAnvilChunkStorageAccessor;
import chunkdebug.utils.ChunkData;
import chunkdebug.utils.ChunkMapSerializer;
import chunkdebug.utils.IChunkTicketManager;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.Identifier;
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
		RELOAD = 15,
		DATA = 16;

	private final Map<ServerPlayerEntity, ServerWorld> validPlayersEnabled = new HashMap<>();
	private final Map<ServerWorld, Set<ChunkData>> serverWorldChunks = new HashMap<>();
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
				case RELOAD -> this.forceReloadChunks();
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
		this.serverWorldChunks.put(world, new HashSet<>());
	}

	public void updateChunkMap(ServerWorld world, ChunkData chunkData) {
		Set<ChunkData> chunkDataSet = this.serverWorldChunks.get(world);
		this.worldsNeedUpdating.add(world);
		chunkDataSet.remove(chunkData);
		if (chunkData.levelType != ChunkHolder.LevelType.INACCESSIBLE) {
			chunkDataSet.add(chunkData);
		}
	}

	public void forceReloadChunks() {
		this.serverWorldChunks.forEach((world, chunkDataSet) -> {
			chunkDataSet.clear();
			ThreadedAnvilChunkStorage storage = world.getChunkManager().threadedAnvilChunkStorage;
			ThreadedAnvilChunkStorage.TicketManager ticketManager = ((ThreadedAnvilChunkStorageAccessor) storage).getTicketManager();
			((ThreadedAnvilChunkStorageAccessor) storage).getChunkHolderMap().values().forEach(chunkHolder -> {
				ChunkHolder.LevelType levelType = ChunkHolder.getLevelType(chunkHolder.getLevel());
				ChunkTicketType<?> ticketType = ((IChunkTicketManager) ticketManager).getTicketType(chunkHolder.getPos().toLong());
				chunkDataSet.add(new ChunkData(chunkHolder.getPos(), levelType, ticketType));
			});
			this.worldsNeedUpdating.add(world);
		});
	}

	private void sendClientUpdate(ServerPlayerEntity player, boolean force) {
		ServerWorld world = this.validPlayersEnabled.get(player);
		if (world == null || (!this.worldsNeedUpdating.contains(world) && !force)) {
			return;
		}
		Set<ChunkData> chunkDataSet = this.serverWorldChunks.get(world);
		if (chunkDataSet == null || chunkDataSet.isEmpty()) {
			return;
		}
		NbtCompound serializedMap = ChunkMapSerializer.serialize(world, chunkDataSet);
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
