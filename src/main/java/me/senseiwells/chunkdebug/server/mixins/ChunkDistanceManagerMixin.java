package me.senseiwells.chunkdebug.server.mixins;

import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;

@Mixin(ChunkMap.DistanceManager.class)
public class ChunkDistanceManagerMixin implements ChunkDebugTrackerHolder {
	@Shadow @Final ChunkMap field_17443;

	@Inject(
		method = "<init>",
		at = @At("TAIL")
	)
	private void synchTickCounters(
		ChunkMap chunkMap,
		Executor dispatcher,
		Executor mainThreadExecutor,
		CallbackInfo ci
	) {
		int ticks = ((ChunkMapAccessor) chunkMap).getLevel().getServer().getTickCount();
		((DistanceManagerAccessor) this).setTickCount(ticks);
	}

	@Override
	public ChunkDebugTracker chunkdebug$getTracker() {
		ServerLevel level = ((ChunkMapAccessor) this.field_17443).getLevel();
		return ((ChunkDebugTrackerHolder) level).chunkdebug$getTracker();
	}
}
