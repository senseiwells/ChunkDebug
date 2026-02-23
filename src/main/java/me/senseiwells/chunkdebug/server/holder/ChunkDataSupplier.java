package me.senseiwells.chunkdebug.server.holder;

import me.senseiwells.chunkdebug.common.utils.MutableChunkData;
import net.minecraft.server.level.ChunkMap;

public interface ChunkDataSupplier {
	MutableChunkData chunkdebug$getChunkData(ChunkMap chunkMap);
}
