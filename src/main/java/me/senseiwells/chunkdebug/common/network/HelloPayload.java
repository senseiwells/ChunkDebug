package me.senseiwells.chunkdebug.common.network;

import io.netty.buffer.ByteBuf;
import me.senseiwells.chunkdebug.ChunkDebug;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record HelloPayload(int version) implements CustomPacketPayload {
	public static final Type<HelloPayload> TYPE = new Type<>(ChunkDebug.id("hello"));
	public static final StreamCodec<ByteBuf, HelloPayload> STREAM_CODEC = ByteBufCodecs.INT.map(HelloPayload::new, HelloPayload::version);

	public static final HelloPayload INSTANCE = new HelloPayload(ChunkDebug.PROTOCOL_VERSION);

	@NotNull
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
