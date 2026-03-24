package me.senseiwells.chunkdebug.server.mixins;

import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkMap.DistanceManager.class)
public class ChunkDistanceManagerMixin implements ChunkDebugTrackerHolder {
	@Shadow @Final ChunkMap this$0;

	@NonNull
	@Override
	public ChunkDebugTracker chunkdebug$getTracker() {

		ServerLevel level = ((ChunkMapAccessor) this.this$0).getLevel();
		return ((ChunkDebugTrackerHolder) level).chunkdebug$getTracker();
	}
}
