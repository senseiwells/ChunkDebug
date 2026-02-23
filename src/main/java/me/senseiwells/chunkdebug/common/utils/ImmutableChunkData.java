package me.senseiwells.chunkdebug.common.utils;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public record ImmutableChunkData(
    ChunkPos position,
    @Nullable ChunkStatus stage,
    List<Ticket> tickets,
    int statusLevel,
    int tickingStatusLevel,
    boolean unloading
) implements ChunkData {
    public static final StreamCodec<RegistryFriendlyByteBuf, ImmutableChunkData> STREAM_CODEC = StreamCodec.of(ImmutableChunkData::encode, ImmutableChunkData::decode);
    public static final StreamCodec<RegistryFriendlyByteBuf, Collection<ImmutableChunkData>> LIST_STREAM_CODEC = ByteBufCodecs.collection(ArrayList::new, STREAM_CODEC);

    public FullChunkStatus status() {
        if (this.tickingStatusLevel > this.statusLevel) {
            return ChunkLevel.fullStatus(this.tickingStatusLevel);
        }
        return ChunkLevel.fullStatus(this.statusLevel);
    }

    public ImmutableChunkData withoutUnloading() {
        return new ImmutableChunkData(
            this.position, this.stage, this.tickets, this.statusLevel, this.tickingStatusLevel, this.unloading
        );
    }

    private static void encode(RegistryFriendlyByteBuf buf, ImmutableChunkData data) {
        buf.writeChunkPos(data.position);
        buf.writeInt(data.statusLevel);
        buf.writeInt(data.tickingStatusLevel);
        buf.writeBoolean(data.unloading);

        ExtraStreamCodecs.OPTIONAL_CHUNK_STATUS.encode(buf, Optional.ofNullable(data.stage));
        ExtraStreamCodecs.TICKETS.encode(buf, data.tickets);
    }

    private static ImmutableChunkData decode(RegistryFriendlyByteBuf buf) {
        ChunkPos pos = buf.readChunkPos();
        int statusLevel = buf.readInt();
        int tickingStatusLevel = buf.readInt();
        boolean unloading = buf.readBoolean();

        ChunkStatus status = ExtraStreamCodecs.OPTIONAL_CHUNK_STATUS.decode(buf).orElse(null);
        List<Ticket> tickets = ExtraStreamCodecs.TICKETS.decode(buf);
        return new ImmutableChunkData(pos, status, tickets, statusLevel, tickingStatusLevel, unloading);
    }

    public record Ticket(TicketType type, int ticketLevel, long ticksLeft) { }
}
