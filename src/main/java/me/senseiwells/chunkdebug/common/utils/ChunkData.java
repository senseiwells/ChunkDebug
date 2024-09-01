package me.senseiwells.chunkdebug.common.utils;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.Ticket;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ChunkData {
	public static final StreamCodec<RegistryFriendlyByteBuf, ChunkData> STREAM_CODEC = StreamCodec.of(ChunkData::encode, ChunkData::decode);
	public static final StreamCodec<RegistryFriendlyByteBuf, Collection<ChunkData>> LIST_STREAM_CODEC = ByteBufCodecs.collection(ArrayList::new, STREAM_CODEC);

	private final ChunkPos position;
	private ChunkStatus stage;
	private List<Ticket<?>> tickets;

	private int statusLevel;
	private int tickingStatusLevel;
	private boolean unloading;

	public ChunkData(
		ChunkPos position,
		ChunkStatus stage,
		SortedArraySet<Ticket<?>> tickets,
		int statusLevel,
		int tickingStatusLevel,
		boolean unloading
	) {
		this(position, stage, ImmutableList.copyOf(tickets), statusLevel, tickingStatusLevel, unloading);
	}

	public ChunkData(
		ChunkPos position,
		ChunkStatus stage,
		List<Ticket<?>> tickets,
		int statusLevel,
		int tickingStatusLevel,
		boolean unloading
	) {
		this.position = position;
		this.stage = stage;
		this.tickets = tickets;

		this.statusLevel = statusLevel;
		this.tickingStatusLevel = tickingStatusLevel;
		this.unloading = unloading;
	}

	public ChunkPos position() {
		return this.position;
	}

	@Nullable
	public ChunkStatus stage() {
		return this.stage;
	}

	public List<Ticket<?>> tickets() {
		return this.tickets;
	}

	public int statusLevel() {
		return this.statusLevel;
	}

	public int tickingStatusLevel() {
		return this.tickingStatusLevel;
	}

	public boolean unloading() {
		return this.unloading;
	}

	public void updateStage(ChunkStatus stage) {
		this.stage = stage;
	}

	public void updateTickets(SortedArraySet<Ticket<?>> tickets) {
		this.tickets = ImmutableList.copyOf(tickets);
	}

	public void updateStatusLevel(int statusLevel) {
		this.statusLevel = statusLevel;
	}

	public void updateTickingStatusLevel(int statusLevel) {
		this.tickingStatusLevel = statusLevel;
	}

	public void updateUnloading(boolean unloading) {
		this.unloading = unloading;
	}

	public FullChunkStatus status() {
		if (this.tickingStatusLevel < 33 || this.tickingStatusLevel > this.statusLevel) {
			return ChunkLevel.fullStatus(this.tickingStatusLevel);
		}
		return ChunkLevel.fullStatus(this.statusLevel);
	}

	private static void encode(RegistryFriendlyByteBuf buf, ChunkData data) {
		buf.writeChunkPos(data.position);
		buf.writeInt(data.statusLevel);
		buf.writeInt(data.tickingStatusLevel);
		buf.writeBoolean(data.unloading);

		Optional<Holder<ChunkStatus>> status = buf.registryAccess().registry(Registries.CHUNK_STATUS)
			.orElse(BuiltInRegistries.CHUNK_STATUS)
			.getResourceKey(data.stage)
			.flatMap(BuiltInRegistries.CHUNK_STATUS::getHolder);
		ExtraStreamCodecs.OPTIONAL_CHUNK_STATUS.encode(buf, status);
		ExtraStreamCodecs.TICKETS.encode(buf, data.tickets);

	}

	private static ChunkData decode(RegistryFriendlyByteBuf buf) {
		ChunkPos pos = buf.readChunkPos();
		int statusLevel = buf.readInt();
		int tickingStatusLevel = buf.readInt();
		boolean unloading = buf.readBoolean();

		ChunkStatus status = ExtraStreamCodecs.OPTIONAL_CHUNK_STATUS.decode(buf).map(Holder::value).orElse(null);
		List<Ticket<?>> tickets = ExtraStreamCodecs.TICKETS.decode(buf);
		return new ChunkData(pos, status, tickets, statusLevel, tickingStatusLevel, unloading);
	}
}