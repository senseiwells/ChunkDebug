package me.senseiwells.chunkdebug.server.mixins.compat.c2me;

import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.flowsched.scheduler.ItemHolder;
import me.senseiwells.chunkdebug.common.utils.MutableChunkData;
import me.senseiwells.chunkdebug.server.holder.ChunkDataSupplier;
import me.senseiwells.chunkdebug.server.mixins.DistanceManagerAccessor;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(NewChunkHolderVanillaInterface.class)
public abstract class NewChunkHolderVanillaInterfaceMixin extends GenerationChunkHolder implements ChunkDataSupplier {
	@Shadow public abstract int getTicketLevel();

	@Shadow(remap = false) @Final private ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> newHolder;

	public NewChunkHolderVanillaInterfaceMixin(ChunkPos pos) {
		super(pos);
	}

	@NotNull
	@Override
	public MutableChunkData chunkdebug$getChunkData(@NonNull ChunkMap chunkMap) {
		DistanceManager manager = chunkMap.getDistanceManager();
		long pos = this.pos.toLong();

		List<Ticket> tickets = ((DistanceManagerAccessor) manager).getTicketsStorage().getTickets(pos);
		int level = this.getTicketLevel();
		int tickingLevel = level;
		if (level == 32 && !chunkMap.getDistanceManager().inBlockTickingRange(pos)) {
			tickingLevel = 33;
		}

		ChunkState state = this.newHolder.getItem().get();
		ChunkAccess chunk = state == null ? null : state.chunk();
		ChunkStatus stage = chunk == null ? null : chunk.getPersistedStatus();
		return new MutableChunkData(this.pos, stage, tickets, level, tickingLevel, false);
	}
}
