package me.senseiwells.chunkdebug.common.network;

import me.senseiwells.chunkdebug.ChunkDebug;
import me.senseiwells.chunkdebug.common.utils.ChunkData;
import me.senseiwells.chunkdebug.common.utils.ExtraStreamCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public record ChunkDataPayload(
	ResourceKey<Level> dimension,
	Collection<ChunkData> chunks,
	int tick,
	boolean initial
) implements CustomPacketPayload {
	public static Type<ChunkDataPayload> TYPE = new Type<>(ChunkDebug.id("chunk_data"));
	public static StreamCodec<RegistryFriendlyByteBuf, ChunkDataPayload> STREAM_CODEC = CustomPacketPayload.codec(
		ChunkDataPayload::encode, ChunkDataPayload::decode
	);

	@NotNull
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private static void encode(ChunkDataPayload payload, RegistryFriendlyByteBuf buf) {
		ExtraStreamCodecs.DIMENSION.encode(buf, payload.dimension);
		ChunkData.LIST_STREAM_CODEC.encode(buf, payload.chunks);
		buf.writeInt(payload.tick);
		buf.writeBoolean(payload.initial);
	}

	private static ChunkDataPayload decode(RegistryFriendlyByteBuf buf) {
		ResourceKey<Level> dimension = ExtraStreamCodecs.DIMENSION.decode(buf);
		Collection<ChunkData> chunks = ChunkData.LIST_STREAM_CODEC.decode(buf);
		int tick = buf.readInt();
		boolean initial = buf.readBoolean();
		return new ChunkDataPayload(dimension, chunks, tick, initial);
	}
}
