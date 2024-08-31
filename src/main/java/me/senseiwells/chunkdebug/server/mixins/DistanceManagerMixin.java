package me.senseiwells.chunkdebug.server.mixins;

import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;

@Mixin(ChunkMap.DistanceManager.class)
public class DistanceManagerMixin {
	@Inject(
		method = "<init>",
		at = @At("TAIL")
	)
	private void synchTickCounters(
		ChunkMap chunkMap,
		Executor dispatcher,
		Executor mainThreadExecutor,
		CallbackInfo ci
	) {
		int ticks = ((ChunkMapAccessor) chunkMap).getLevel().getServer().getTickCount();
		((DistanceManagerAccessor) this).setTickCount(ticks);
	}
}
