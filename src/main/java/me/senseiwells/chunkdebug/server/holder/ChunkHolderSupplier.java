package me.senseiwells.chunkdebug.server.holder;

import net.minecraft.server.level.ChunkHolder;

import java.util.function.Consumer;

public interface ChunkHolderSupplier {
	void chunkdebug$forEachChunkHolder(Consumer<ChunkHolder> consumer);
}
