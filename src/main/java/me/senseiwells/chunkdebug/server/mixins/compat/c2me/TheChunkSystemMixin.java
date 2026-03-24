package me.senseiwells.chunkdebug.server.mixins.compat.c2me;

import com.ishland.c2me.rewrites.chunksystem.common.*;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.ItemStatus;
import me.senseiwells.chunkdebug.common.utils.MutableChunkData;
import me.senseiwells.chunkdebug.server.holder.ChunkDataSupplier;
import me.senseiwells.chunkdebug.server.mixins.ChunkMapAccessor;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import me.senseiwells.chunkdebug.server.utils.LevelUtils;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TheChunkSystem.class)
public class TheChunkSystemMixin {
	@Shadow @Final private ChunkMap tacs;

	@Inject(
		method = {"onItemUpgrade", "onItemDowngrade"},
		at = @At("TAIL"),
		remap = false
	)
	private void onChunkUpgrade(
		ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder,
		ItemStatus<ChunkPos, ChunkState, ChunkLoadingContext> statusReached,
		CallbackInfo ci
	) {
		ServerLevel level = ((ChunkMapAccessor) this.tacs).getLevel();
		ChunkDebugTracker tracker = ((ChunkDebugTrackerHolder) level).chunkdebug$getTracker();
		NewChunkHolderVanillaInterface chunk = holder.getUserData().get();
		LevelUtils.execute(level, () -> {
			MutableChunkData data = ((ChunkDataSupplier) chunk).chunkdebug$getChunkData(this.tacs);
			tracker.set(data);
		});
	}

	@Inject(
		method = "onItemRemoval",
		at = @At("TAIL"),
		remap = false
	)
	private void onChunkUnload(
		ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder,
		CallbackInfo ci
	) {
		long pos = holder.getKey().pack();
		ServerLevel level = ((ChunkMapAccessor) this.tacs).getLevel();
		ChunkDebugTracker tracker = ((ChunkDebugTrackerHolder) level).chunkdebug$getTracker();
		LevelUtils.execute(level, () -> tracker.unload(pos));
	}
}
