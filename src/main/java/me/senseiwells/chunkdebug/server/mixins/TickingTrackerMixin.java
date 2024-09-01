package me.senseiwells.chunkdebug.server.mixins;

import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.MutableChunkDebugTrackerHolder;
import net.minecraft.server.level.TickingTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TickingTracker.class)
public class TickingTrackerMixin implements MutableChunkDebugTrackerHolder {
	@Unique private ChunkDebugTracker chunkdebug$tracker;

	@Inject(
		method = "setLevel",
		at = @At("TAIL")
	)
	private void onSetTickingLevel(long pos, int level, CallbackInfo ci) {
		this.chunkdebug$tracker.updateTickingStatusLevel(pos, Math.min(level, 33));
	}

	@Override
	public void chunkdebug$setTracker(ChunkDebugTracker tracker) {
		this.chunkdebug$tracker = tracker;
	}

	@Override
	public ChunkDebugTracker chunkdebug$getTracker() {
		return this.chunkdebug$tracker;
	}
}
