package me.senseiwells.chunkdebug.server.holder;

import me.senseiwells.chunkdebug.common.utils.ChunkData;
import net.minecraft.server.level.ChunkMap;

public interface ChunkDataSupplier {
	ChunkData chunkdebug$getChunkData(ChunkMap chunkMap);
}
