package chunkdebug.feature;

import chunkdebug.ChunkDebugServer;
import chunkdebug.mixins.ThreadedAnvilChunkStorageAccessor;
import chunkdebug.utils.ChunkData;
import chunkdebug.utils.IChunkTicketManager;
import com.google.common.collect.Iterables;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.*;

public class ChunkServerNetworkHandler {
	public static Identifier ESSENTIAL_CHANNEL = new Identifier("essentialclient", "chunkdebug");
	public static final int
		HELLO = 0,
		RELOAD = 15,
		DATA = 16,
		VERSION = 1_0_3;

	private final Map<ServerPlayerEntity, ServerWorld> validPlayersEnabled = new HashMap<>();
	private final Map<ServerWorld, Set<ChunkData>> serverWorldChunks = new HashMap<>();
	private final Map<ServerWorld, Set<ChunkData>> updatesInLastTick = new HashMap<>();

	public void onHello(ServerPlayerEntity player, PacketByteBuf packetByteBuf) {
		String essentialVersion = packetByteBuf.readString(32767);
		if (packetByteBuf.readableBytes() == 0 || packetByteBuf.readVarInt() < VERSION) {
			player.sendMessage(Text.of("You cannot use ChunkDebug, client out of date"), false);
			return;
		}
		this.validPlayersEnabled.put(player, null);
		ChunkDebugServer.LOGGER.info("%s has logged in with ChunkDebug. EssentialClient %s".formatted(player.getEntityName(), essentialVersion));
	}

	public void removePlayer(ServerPlayerEntity player) {
		this.validPlayersEnabled.remove(player);
	}

	public void handlePacket(PacketByteBuf packetByteBuf, ServerPlayerEntity player) {
		if (packetByteBuf != null) {
			switch (packetByteBuf.readVarInt()) {
				case HELLO -> this.onHello(player, packetByteBuf);
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
		this.updatesInLastTick.put(world, new HashSet<>());
	}

	public synchronized void updateChunkMap(ServerWorld world, ChunkData chunkData) {
		Set<ChunkData> chunkDataSet = this.serverWorldChunks.get(world);
		Set<ChunkData> tickChunkDataSet = this.updatesInLastTick.get(world);
		chunkDataSet.remove(chunkData);
		tickChunkDataSet.remove(chunkData);
		if (!chunkData.isLevelType(ChunkHolder.LevelType.INACCESSIBLE)) {
			chunkDataSet.add(chunkData);
		}
		tickChunkDataSet.add(chunkData);
	}

	public synchronized void forceReloadChunks() {
		this.serverWorldChunks.forEach((world, chunkDataSet) -> {
			chunkDataSet.clear();
			ThreadedAnvilChunkStorage storage = world.getChunkManager().threadedAnvilChunkStorage;
			ThreadedAnvilChunkStorage.TicketManager ticketManager = ((ThreadedAnvilChunkStorageAccessor) storage).getTicketManager();
			((ThreadedAnvilChunkStorageAccessor) storage).getChunkHolderMap().values().forEach(chunkHolder -> {
				ChunkPos pos = chunkHolder.getPos();
				ChunkHolder.LevelType levelType = ChunkHolder.getLevelType(chunkHolder.getLevel());
				long posLong = pos.toLong();
				ChunkTicketType<?> ticketType = ((IChunkTicketManager) ticketManager).getTicketType(posLong);
				if (levelType == ChunkHolder.LevelType.ENTITY_TICKING && !ticketManager.shouldTickEntities(posLong)) {
					levelType = ChunkHolder.LevelType.TICKING;
					ticketType = null;
				}
				Chunk chunk = chunkHolder.getCurrentChunk();
				ChunkStatus status = chunk == null ? ChunkStatus.EMPTY : chunk.getStatus();
				chunkDataSet.add(new ChunkData(pos, levelType, status, ticketType));
			});
			this.updatesInLastTick.get(world).addAll(chunkDataSet);
		});
	}

	private synchronized void sendClientUpdate(ServerPlayerEntity player, boolean force) {
		ServerWorld world = this.validPlayersEnabled.get(player);
		if (world == null) {
			return;
		}
		Set<ChunkData> chunkDataSet = force ? this.serverWorldChunks.get(world) : this.updatesInLastTick.get(world);
		if (chunkDataSet == null || chunkDataSet.isEmpty()) {
			return;
		}
		if (chunkDataSet.size() > 100000) {
			for (List<ChunkData> subChunkDataSet : Iterables.partition(chunkDataSet, 100000)) {
				this.sendClientChunkData(player, world, subChunkDataSet);
			}
			return;
		}
		this.sendClientChunkData(player, world, chunkDataSet);
	}

	private void sendClientChunkData(ServerPlayerEntity player, ServerWorld world, Collection<ChunkData> chunkDataCollection) {
		int size = chunkDataCollection.size();
		long[] chunkPositions = new long[size];
		byte[] levelTypes = new byte[size];
		byte[] statusTypes = new byte[size];
		byte[] ticketTypes = new byte[size];
		int i = 0;
		for (ChunkData chunkData : chunkDataCollection) {
			chunkPositions[i] = chunkData.getLongPos();
			levelTypes[i] = chunkData.getLevelByte();
			statusTypes[i] = chunkData.getStatusByte();
			ticketTypes[i] = chunkData.getTicketByte();
			i++;
		}
		player.networkHandler.sendPacket(new CustomPayloadS2CPacket(
			ESSENTIAL_CHANNEL,
			new PacketByteBuf(Unpooled.buffer()).writeVarInt(DATA)
				.writeVarInt(size).writeLongArray(chunkPositions).writeByteArray(levelTypes)
				.writeByteArray(statusTypes).writeByteArray(ticketTypes)
				.writeString(world.getRegistryKey().getValue().getPath())
		));
	}

	private void forceUpdateChunkData() {
		this.serverWorldChunks.forEach((world, chunks) -> {
			if (this.validPlayersEnabled.containsValue(world)) {
				ServerChunkManager chunkManager = world.getChunkManager();
				ChunkTicketManager manager = chunkManager.threadedAnvilChunkStorage.getTicketManager();
				List<ChunkData> updatedChunks = new LinkedList<>();
				for (ChunkData chunkData : chunks) {
					long longPos = chunkData.getLongPos();
					boolean entityTicking = manager.shouldTickEntities(longPos);
					boolean dirty = false;
					ChunkHolder.LevelType newType;
					if (chunkData.isLevelType(ChunkHolder.LevelType.TICKING) && entityTicking) {
						newType = ChunkHolder.LevelType.ENTITY_TICKING;
						dirty = true;
					} else if (chunkData.isLevelType(ChunkHolder.LevelType.ENTITY_TICKING) && !entityTicking) {
						newType = ChunkHolder.LevelType.TICKING;
						dirty = true;
					} else {
						newType = chunkData.getLevelType();
					}

					byte ticket = ChunkData.getTicketCode(((IChunkTicketManager) manager).getTicketType(longPos));
					if (chunkData.getTicketByte() != ticket) {
						dirty = true;
					}

					ChunkPos chunkPos = chunkData.getChunkPos();
					Chunk chunk = chunkManager.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.EMPTY, false);
					byte status = (byte) (chunk == null ? ChunkStatus.EMPTY : chunk.getStatus()).getIndex();
					if (chunkData.getStatusByte() != status) {
						dirty = true;
					}

					if (dirty) {
						updatedChunks.add(new ChunkData(chunkData.getChunkPos(), newType, status, ticket));
					}
				}
				for (ChunkData updatedChunk : updatedChunks) {
					this.updateChunkMap(world, updatedChunk);
				}
			}
		});
	}

	public void tickUpdate() {
		this.forceUpdateChunkData();
		for (ServerPlayerEntity players : this.validPlayersEnabled.keySet()) {
			this.sendClientUpdate(players, false);
		}
		this.updatesInLastTick.values().forEach(Set::clear);
	}
}
