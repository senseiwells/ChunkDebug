package me.senseiwells.chunkdebug.server.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStep;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
	@Shadow @Final ServerLevel level;

	@Inject(
		method = "processUnloads",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/ChunkMap;scheduleUnload(JLnet/minecraft/server/level/ChunkHolder;)V"
		)
	)
	private void onScheduleUnloadHolder(BooleanSupplier hasMoreTime, CallbackInfo ci, @Local long pos) {
		ChunkDebugTracker tracker = ((ChunkDebugTrackerHolder) this.level).chunkdebug$getTracker();
		tracker.updateUnloading(pos, true);
	}

	@Inject(
		method = "method_60440",
		at = @At(
			value = "INVOKE",
			target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;remove(JLjava/lang/Object;)Z"
		)
	)
	private void onUnloadHolder(ChunkHolder chunkHolder, long l, CallbackInfo ci) {
		ChunkDebugTracker tracker = ((ChunkDebugTrackerHolder) this.level).chunkdebug$getTracker();
		tracker.unload(l);
	}

	@Inject(
		method = "applyStep",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/progress/ChunkProgressListener;onStatusChange(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/status/ChunkStatus;)V"
		)
	)
	private void onStatusChange(
		GenerationChunkHolder chunk,
		ChunkStep step,
		StaticCache2D<GenerationChunkHolder> cache,
		CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
	) {
		ChunkDebugTracker tracker = ((ChunkDebugTrackerHolder) this.level).chunkdebug$getTracker();
		tracker.updateStage(chunk.getPos().toLong(), step.targetStatus());
	}
}
