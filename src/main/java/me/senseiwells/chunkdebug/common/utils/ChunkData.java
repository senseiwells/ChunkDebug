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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record ChunkData(
	ChunkPos position,
	int statusLevel,
	int tickingStatusLevel,
	ChunkStatus stage,
	List<Ticket<?>> tickets
) {
	public static final StreamCodec<RegistryFriendlyByteBuf, ChunkData> STREAM_CODEC = StreamCodec.of(ChunkData::encode, ChunkData::decode);
	public static final StreamCodec<RegistryFriendlyByteBuf, Collection<ChunkData>> LIST_STREAM_CODEC = ByteBufCodecs.collection(ArrayList::new, STREAM_CODEC);

	public ChunkData(
		ChunkPos position,
		int statusLevel,
		int tickingStatusLevel,
		ChunkStatus stage,
		SortedArraySet<Ticket<?>> tickets
	) {
		this(position, statusLevel, tickingStatusLevel, stage, ImmutableList.copyOf(tickets));
	}

	public FullChunkStatus status() {
		return ChunkLevel.fullStatus(this.statusLevel);
	}

	private static void encode(RegistryFriendlyByteBuf buf, ChunkData data) {
		buf.writeChunkPos(data.position);
		buf.writeInt(data.statusLevel);
		buf.writeInt(data.tickingStatusLevel);

		Holder<ChunkStatus> status = buf.registryAccess().registry(Registries.CHUNK_STATUS)
			.orElse(BuiltInRegistries.CHUNK_STATUS)
			.getResourceKey(data.stage)
			.flatMap(BuiltInRegistries.CHUNK_STATUS::getHolder)
			.orElseThrow();
		ExtraStreamCodecs.CHUNK_STATUS.encode(buf, status);
		ExtraStreamCodecs.TICKETS.encode(buf, data.tickets);
	}

	private static ChunkData decode(RegistryFriendlyByteBuf buf) {
		ChunkPos pos = buf.readChunkPos();
		int statusLevel = buf.readInt();
		int tickingStatusLevel = buf.readInt();
		ChunkStatus status = ExtraStreamCodecs.CHUNK_STATUS.decode(buf).value();
		List<Ticket<?>> tickets = ExtraStreamCodecs.TICKETS.decode(buf);
		return new ChunkData(pos, statusLevel, tickingStatusLevel, status, tickets);
	}
}