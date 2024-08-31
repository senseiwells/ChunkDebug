package me.senseiwells.chunkdebug.server.mixins;

import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import me.senseiwells.chunkdebug.common.utils.ChunkData;
import net.minecraft.server.level.*;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin extends GenerationChunkHolder {
	@Shadow private int ticketLevel;

	public ChunkHolderMixin(ChunkPos pos) {
		super(pos);
	}

	@Inject(method = "updateFutures", at = @At("RETURN"))
	private void onTick(ChunkMap chunkMap, Executor executor, CallbackInfo ci) {
		ServerLevel level = ((ChunkMapAccessor) chunkMap).getLevel();
		DistanceManager manager = chunkMap.getDistanceManager();
		long posLong = this.pos.toLong();

		SortedArraySet<Ticket<?>> tickets = ((DistanceManagerAccessor) manager).getTicketsFor(posLong);
		int statusLevel = ((DistanceManagerAccessor) manager).getTickingTracker().getLevel(this.pos);

		ChunkStatus stage = this.getPersistedStatus();
		if (stage == null) {
			stage = ChunkStatus.EMPTY;
		}
		ChunkDebugTracker tracker = ((ChunkDebugTrackerHolder) level).chunkdebug$getTracker();
		tracker.update(new ChunkData(this.pos, this.ticketLevel, statusLevel, stage, tickets));
	}
}
