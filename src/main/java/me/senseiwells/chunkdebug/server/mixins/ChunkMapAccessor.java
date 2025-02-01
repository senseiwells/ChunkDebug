package me.senseiwells.chunkdebug.server.mixins;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
	@Accessor("level")
	ServerLevel getLevel();

	// We have this accessor here and not in ServerChunkCache
	// to avoid the AW needed due to the package-private type
	@Accessor
	BlockableEventLoop<Runnable> getMainThreadExecutor();
}
