package me.senseiwells.chunkdebug.server.mixins;

import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import me.senseiwells.chunkdebug.server.tracker.MutableChunkDebugTrackerHolder;
import net.minecraft.server.level.*;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;

@Mixin(DistanceManager.class)
public class DistanceManagerMixin {
	@Shadow @Final private SimulationChunkTracker simulationChunkTracker;
	@Shadow @Final private LoadingChunkTracker loadingChunkTracker;
	@Shadow @Final TicketStorage ticketStorage;

	@Inject(
		method = "<init>",
		at = @At("TAIL")
	)
	private void onInit(TicketStorage ticketStorage, Executor background, Executor main, CallbackInfo ci) {
		if (this instanceof ChunkDebugTrackerHolder holder) {
			ChunkDebugTracker tracker = holder.chunkdebug$getTracker();
			((MutableChunkDebugTrackerHolder) this.simulationChunkTracker).chunkdebug$setTracker(tracker);
			((MutableChunkDebugTrackerHolder) this.loadingChunkTracker).chunkdebug$setTracker(tracker);
			((MutableChunkDebugTrackerHolder) this.ticketStorage).chunkdebug$setTracker(tracker);
		}
	}
}
