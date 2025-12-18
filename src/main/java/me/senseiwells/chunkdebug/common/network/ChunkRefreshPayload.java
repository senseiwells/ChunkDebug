package me.senseiwells.chunkdebug.common.network;

import io.netty.buffer.ByteBuf;
import me.senseiwells.chunkdebug.ChunkDebug;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public enum ChunkRefreshPayload implements CustomPacketPayload {
	INSTANCE;

	public static final Type<ChunkRefreshPayload> TYPE = new Type<>(ChunkDebug.id("refresh"));
	public static final StreamCodec<ByteBuf, ChunkRefreshPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
