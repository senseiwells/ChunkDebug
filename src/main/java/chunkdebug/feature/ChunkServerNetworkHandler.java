package chunkdebug.feature;

import chunkdebug.ChunkDebugServer;
import chunkdebug.mixins.ThreadedAnvilChunkStorageAccessor;
import chunkdebug.utils.ChunkData;
import chunkdebug.utils.IChunkTicketManager;
import com.google.common.collect.Iterables;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.*;

public class ChunkServerNetworkHandler {
	public static Identifier ESSENTIAL_CHANNEL = new Identifier("essentialclient", "chunkdebug");
	public static final int
		HELLO = 0,
		STOP = 14,
		RELOAD = 15,
		DATA = 16,
		VERSION = 1_0_4;

	private final Map<ServerPlayNetworkHandler, ServerWorld> validPlayersEnabled = new HashMap<>();
	private final Map<ServerWorld, Set<ChunkData>> serverWorldChunks = new HashMap<>();
	private final Map<ServerWorld, Set<ChunkData>> updatesInLastTick = new HashMap<>();

	public void tick() {
		this.forceUpdateChunkData();
		for (ServerPlayNetworkHandler networkHandler : this.validPlayersEnabled.keySet()) {
			this.sendClientUpdate(networkHandler, false);
		}
		this.updatesInLastTick.values().forEach(Set::clear);
	}

	public void sayHello(ServerPlayerEntity player) {
		ServerPlayNetworking.send(
			player,
			ChunkServerNetworkHandler.ESSENTIAL_CHANNEL,
			new PacketByteBuf(Unpooled.buffer())
				.writeVarInt(ChunkServerNetworkHandler.HELLO)
				.writeVarInt(ChunkServerNetworkHandler.VERSION)
		);
	}

	public void onHello(ServerPlayerEntity player, PacketByteBuf packetByteBuf) {
		String essentialVersion = packetByteBuf.readString(32767);
		if (packetByteBuf.readableBytes() == 0 || packetByteBuf.readVarInt() < VERSION) {
			player.sendMessage(Text.of("You cannot use ChunkDebug, client out of date"), false);
			return;
		}
		this.validPlayersEnabled.put(player.networkHandler, null);
		ChunkDebugServer.LOGGER.info("%s has logged in with ChunkDebug. EssentialClient %s".formatted(player.getNameForScoreboard(), essentialVersion));
	}

	public void removeHandler(ServerPlayNetworkHandler handler) {
		this.validPlayersEnabled.remove(handler);
	}

	public void handlePacket(PacketByteBuf packetByteBuf, ServerPlayerEntity player) {
		if (packetByteBuf != null) {
			switch (packetByteBuf.readVarInt()) {
				case HELLO -> this.onHello(player, packetByteBuf);
				case DATA -> this.processPacket(packetByteBuf, player);
				case RELOAD -> this.forceReloadChunks();
				case STOP -> this.stopSendingToPlayer(player);
			}
		}
	}

	private void processPacket(PacketByteBuf packetByteBuf, ServerPlayerEntity player) {
		if (this.validPlayersEnabled.containsKey(player.networkHandler)) {
			RegistryKey<World> world = packetByteBuf.readRegistryKey(RegistryKeys.WORLD);
			this.validPlayersEnabled.replace(
				player.networkHandler,
				player.server.getWorld(world)
			);
			this.sendClientUpdate(player.networkHandler, true);
		}
	}

	private void stopSendingToPlayer(ServerPlayerEntity player) {
		if (this.validPlayersEnabled.containsKey(player.networkHandler)) {
			this.validPlayersEnabled.replace(player.networkHandler, null);
		}
	}

	public void addWorld(ServerWorld world) {
		this.serverWorldChunks.put(world, new HashSet<>());
		this.updatesInLastTick.put(world, new HashSet<>());
	}

	public void updateChunkMap(ServerWorld world, ChunkData chunkData) {
		Set<ChunkData> chunkDataSet = this.serverWorldChunks.get(world);
		Set<ChunkData> tickChunkDataSet = this.updatesInLastTick.get(world);
		chunkDataSet.remove(chunkData);
		tickChunkDataSet.remove(chunkData);
		if (!chunkData.isLevelType(ChunkLevelType.INACCESSIBLE)) {
			chunkDataSet.add(chunkData);
		}
		tickChunkDataSet.add(chunkData);
	}

	public void forceReloadChunks() {
		this.serverWorldChunks.forEach((world, chunkDataSet) -> {
			chunkDataSet.clear();
			ThreadedAnvilChunkStorage storage = world.getChunkManager().threadedAnvilChunkStorage;
			ThreadedAnvilChunkStorage.TicketManager ticketManager = ((ThreadedAnvilChunkStorageAccessor) storage).getTicketManager();
			((ThreadedAnvilChunkStorageAccessor) storage).getChunkHolderMap().values().forEach(chunkHolder -> {
				ChunkPos pos = chunkHolder.getPos();
				ChunkLevelType levelType = chunkHolder.getLevelType();
				long posLong = pos.toLong();
				ChunkTicketType<?> ticketType = ((IChunkTicketManager) ticketManager).chunkdebug$getTicketType(posLong);
				if (levelType == ChunkLevelType.ENTITY_TICKING && !ticketManager.shouldTickEntities(posLong)) {
					levelType = ChunkLevelType.BLOCK_TICKING;
					ticketType = null;
				}
				ChunkStatus status = this.getChunkStatus(chunkHolder);
				chunkDataSet.add(new ChunkData(pos, levelType, status == null ? ChunkStatus.EMPTY : status, ticketType));
			});
			this.updatesInLastTick.get(world).addAll(chunkDataSet);
		});
	}

	private void sendClientUpdate(ServerPlayNetworkHandler networkHandler, boolean force) {
		ServerWorld world = this.validPlayersEnabled.get(networkHandler);
		if (world == null) {
			return;
		}
		Set<ChunkData> chunkDataSet = force ? this.serverWorldChunks.get(world) : this.updatesInLastTick.get(world);
		if (chunkDataSet == null || chunkDataSet.isEmpty()) {
			return;
		}
		if (chunkDataSet.size() > 100000) {
			for (List<ChunkData> subChunkDataSet : Iterables.partition(chunkDataSet, 100000)) {
				this.sendClientChunkData(networkHandler, world, subChunkDataSet);
			}
			return;
		}
		this.sendClientChunkData(networkHandler, world, chunkDataSet);
	}

	private void sendClientChunkData(ServerPlayNetworkHandler networkHandler, ServerWorld world, Collection<ChunkData> chunkDataCollection) {
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
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(DATA)
			.writeVarInt(size).writeLongArray(chunkPositions).writeByteArray(levelTypes)
			.writeByteArray(statusTypes).writeByteArray(ticketTypes)
			.writeRegistryKey(world.getRegistryKey());
		ServerPlayNetworking.send(networkHandler.player, ESSENTIAL_CHANNEL, buf);
	}

	private void forceUpdateChunkData() {
		this.serverWorldChunks.forEach((world, chunks) -> {
			if (!this.validPlayersEnabled.containsValue(world)) {
				return;
			}
			ServerChunkManager chunkManager = world.getChunkManager();
			ThreadedAnvilChunkStorageAccessor storageAccessor = (ThreadedAnvilChunkStorageAccessor) chunkManager.threadedAnvilChunkStorage;
			ChunkTicketManager manager = storageAccessor.getTicketManager();
			Long2ObjectLinkedOpenHashMap<ChunkHolder> holders = storageAccessor.getChunkHolderMap();
			List<ChunkData> updatedChunks = new LinkedList<>();
			for (ChunkData chunkData : chunks) {
				long longPos = chunkData.getLongPos();
				boolean dirty;
				ChunkHolder holder = holders.get(longPos);
				boolean entityTicking = manager.shouldTickEntities(longPos);
				ChunkLevelType type;
				if (chunkData.isLevelType(ChunkLevelType.BLOCK_TICKING) && entityTicking) {
					type = ChunkLevelType.ENTITY_TICKING;
					dirty = true;
				} else if (chunkData.isLevelType(ChunkLevelType.ENTITY_TICKING) && !entityTicking) {
					type = ChunkLevelType.BLOCK_TICKING;
					dirty = true;
				} else {
					type = holder.getLevelType();
					dirty = chunkData.getLevelType() == type;
				}

				byte ticket = ChunkData.getTicketCode(((IChunkTicketManager) manager).chunkdebug$getTicketType(longPos));
				if (chunkData.getTicketByte() != ticket) {
					dirty = true;
				}

				ChunkStatus status = this.getChunkStatus(holder);
				byte statusByte = (byte) (status == null ? ChunkStatus.EMPTY : status).getIndex();
				if (chunkData.getStatusByte() != statusByte) {
					dirty = true;
				}

				if (dirty) {
					updatedChunks.add(new ChunkData(chunkData.getChunkPos(), type, statusByte, ticket));
				}
			}
			for (ChunkData updatedChunk : updatedChunks) {
				this.updateChunkMap(world, updatedChunk);
			}
		});
	}

	private ChunkStatus getChunkStatus(ChunkHolder holder) {
		return holder.getCurrentStatus();
	}
}


