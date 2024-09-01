package me.senseiwells.chunkdebug.server.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import me.senseiwells.chunkdebug.server.tracker.MutableChunkDebugTrackerHolder;
import net.minecraft.server.level.*;
import net.minecraft.util.SortedArraySet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;

@Mixin(DistanceManager.class)
public class DistanceManagerMixin {
	@Shadow @Final private TickingTracker tickingTicketsTracker;

	@Inject(
		method = "<init>",
		at = @At("TAIL")
	)
	private void onInit(Executor dispatcher, Executor mainThreadExecutor, CallbackInfo ci) {
		if (this instanceof ChunkDebugTrackerHolder holder) {
			ChunkDebugTracker tracker = holder.chunkdebug$getTracker();
			((MutableChunkDebugTrackerHolder) this.tickingTicketsTracker).chunkdebug$setTracker(tracker);
		}
	}

	@Inject(
		method = {
			"addTicket(JLnet/minecraft/server/level/Ticket;)V",
			"removeTicket(JLnet/minecraft/server/level/Ticket;)V"
		},
		at = @At("TAIL")
	)
	private void onAddTicket(long chunkPos, Ticket<?> ticket, CallbackInfo ci, @Local SortedArraySet<Ticket<?>> tickets) {
		if ((Object) this instanceof ChunkDebugTrackerHolder holder) {
			ChunkDebugTracker tracker = holder.chunkdebug$getTracker();
			tracker.updateTickets(chunkPos, tickets);
		}
	}
}
