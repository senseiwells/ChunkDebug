package me.senseiwells.chunkdebug.common.network;

import me.senseiwells.chunkdebug.ChunkDebug;
import me.senseiwells.chunkdebug.common.utils.ExtraStreamCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record ChunkUnloadPayload(
	ResourceKey<Level> dimension,
	long[] positions
) implements CustomPacketPayload {
	public static Type<ChunkUnloadPayload> TYPE = new Type<>(ChunkDebug.id("chunk_unload"));
	public static StreamCodec<RegistryFriendlyByteBuf, ChunkUnloadPayload> STREAM_CODEC = CustomPacketPayload.codec(
		ChunkUnloadPayload::encode, ChunkUnloadPayload::decode
	);

	@NotNull
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private static void encode(ChunkUnloadPayload payload, RegistryFriendlyByteBuf buf) {
		ExtraStreamCodecs.DIMENSION.encode(buf, payload.dimension);
		buf.writeLongArray(payload.positions);
	}

	private static ChunkUnloadPayload decode(RegistryFriendlyByteBuf buf) {
		ResourceKey<Level> dimension = ExtraStreamCodecs.DIMENSION.decode(buf);
		long[] positions = buf.readLongArray();
		return new ChunkUnloadPayload(dimension, positions);
	}
}