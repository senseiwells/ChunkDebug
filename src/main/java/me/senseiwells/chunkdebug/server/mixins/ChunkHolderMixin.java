package me.senseiwells.chunkdebug.server.mixins;

import me.senseiwells.chunkdebug.server.holder.ChunkDataSupplier;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import me.senseiwells.chunkdebug.common.utils.MutableChunkData;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin extends GenerationChunkHolder implements ChunkDataSupplier {
	@Shadow public abstract int getTicketLevel();

	public ChunkHolderMixin(ChunkPos pos) {
		super(pos);
	}

	@Inject(method = "updateFutures", at = @At("RETURN"))
	private void onTick(ChunkMap chunkMap, Executor executor, CallbackInfo ci) {
		ServerLevel level = ((ChunkMapAccessor) chunkMap).getLevel();
		ChunkDebugTracker tracker = ((ChunkDebugTrackerHolder) level).chunkdebug$getTracker();
		tracker.set(this.chunkdebug$getChunkData(chunkMap));
	}

	@NonNull
	@Override
	public MutableChunkData chunkdebug$getChunkData(@NonNull ChunkMap chunkMap) {
		DistanceManager manager = chunkMap.getDistanceManager();
		long pos = this.pos.toLong();

		List<Ticket> tickets = ((DistanceManagerAccessor) manager).getTicketsStorage().getTickets(pos);
		int statusLevel = ((DistanceManagerAccessor) manager).getSimulationChunkTracker().getLevel(this.pos);

		ChunkStatus stage = this.getPersistedStatus();
		return new MutableChunkData(this.pos, stage, tickets, this.getTicketLevel(), statusLevel, false);
	}
}
