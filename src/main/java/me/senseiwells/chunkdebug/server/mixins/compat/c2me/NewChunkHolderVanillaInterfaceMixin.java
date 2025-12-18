package me.senseiwells.chunkdebug.server.mixins.compat.c2me;

import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.flowsched.scheduler.ItemHolder;
import me.senseiwells.chunkdebug.common.utils.ChunkData;
import me.senseiwells.chunkdebug.server.holder.ChunkDataSupplier;
import me.senseiwells.chunkdebug.server.mixins.ChunkMapAccessor;
import me.senseiwells.chunkdebug.server.mixins.DistanceManagerAccessor;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(NewChunkHolderVanillaInterface.class)
public abstract class NewChunkHolderVanillaInterfaceMixin extends GenerationChunkHolder implements ChunkDataSupplier {
	@Shadow public abstract int getTicketLevel();

	@Shadow(remap = false) @Final private ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> newHolder;

	public NewChunkHolderVanillaInterfaceMixin(ChunkPos pos) {
		super(pos);
	}

	@Inject(
		method = "updateFutures",
		at = @At("RETURN")
	)
	private void onTick(ChunkMap chunkMap, Executor executor, CallbackInfo ci) {
		ServerLevel level = ((ChunkMapAccessor) chunkMap).getLevel();
		ChunkDebugTracker tracker = ((ChunkDebugTrackerHolder) level).chunkdebug$getTracker();
		tracker.set(this.chunkdebug$getChunkData(chunkMap));
	}

	@NotNull
	@Override
	public ChunkData chunkdebug$getChunkData(@NonNull ChunkMap chunkMap) {
		DistanceManager manager = chunkMap.getDistanceManager();
		long pos = this.pos.toLong();

		List<Ticket> tickets = ((DistanceManagerAccessor) manager).getTicketsStorage().getTickets(pos);
		int statusLevel = ((DistanceManagerAccessor) manager).getSimulationChunkTracker().getLevel(this.pos);

		ChunkState state = this.newHolder.getItem().get();
		ChunkAccess chunk = state == null ? null : state.chunk();
		ChunkStatus stage = chunk == null ? null : chunk.getPersistedStatus();
		return new ChunkData(this.pos, stage, tickets, this.getTicketLevel(), statusLevel, false);
	}
}
