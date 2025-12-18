package me.senseiwells.chunkdebug.server.mixins.compat.c2me;

import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.MutableChunkDebugTrackerHolder;
import net.minecraft.server.level.LoadingChunkTracker;
import net.minecraft.server.level.SimulationChunkTracker;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({LoadingChunkTracker.class, SimulationChunkTracker.class})
public class TickingTrackerMixin implements MutableChunkDebugTrackerHolder {
	@Override
	public void chunkdebug$setTracker(@NonNull ChunkDebugTracker tracker) {

	}

	@NonNull
	@Override
	public ChunkDebugTracker chunkdebug$getTracker() {
		throw new UnsupportedOperationException();
	}
}
