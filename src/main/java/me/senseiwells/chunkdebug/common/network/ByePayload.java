package me.senseiwells.chunkdebug.common.network;

import io.netty.buffer.ByteBuf;
import me.senseiwells.chunkdebug.ChunkDebug;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public enum ByePayload implements CustomPacketPayload {
	INSTANCE;

	public static final Type<ByePayload> TYPE = new Type<>(ChunkDebug.id("bye"));
	public static final StreamCodec<ByteBuf, ByePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
