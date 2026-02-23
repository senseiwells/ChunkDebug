package me.senseiwells.chunkdebug.common.utils;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

public sealed interface ChunkData permits ImmutableChunkData, MutableChunkData {
    ChunkPos position();

    @Nullable ChunkStatus stage();

    int statusLevel();

    int tickingStatusLevel();

    boolean unloading();
}
