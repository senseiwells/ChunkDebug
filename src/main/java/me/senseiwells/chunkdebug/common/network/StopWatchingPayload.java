package me.senseiwells.chunkdebug.common.network;

import me.senseiwells.chunkdebug.ChunkDebug;
import me.senseiwells.chunkdebug.common.utils.ExtraStreamCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record StopWatchingPayload(List<ResourceKey<Level>> dimensions) implements CustomPacketPayload {
	public static final Type<StopWatchingPayload> TYPE = new Type<>(ChunkDebug.id("stop_watching"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StopWatchingPayload> STREAM_CODEC = ExtraStreamCodecs.DIMENSIONS.map(StopWatchingPayload::new, StopWatchingPayload::dimensions);

	public StopWatchingPayload() {
		this(List.of());
	}

	@NotNull
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}