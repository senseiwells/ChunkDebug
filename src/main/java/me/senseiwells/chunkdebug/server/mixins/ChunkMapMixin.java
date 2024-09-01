package me.senseiwells.chunkdebug.server.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
}
