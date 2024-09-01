package me.senseiwells.chunkdebug.server.mixins;

import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public class ServerLevelMixin implements ChunkDebugTrackerHolder {
	@Unique private final ChunkDebugTracker chunkdebug$tracker = new ChunkDebugTracker((ServerLevel) (Object) this);

	@Inject(
		method = "tick",
		at = @At("HEAD")
	)
	private void onTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
		this.chunkdebug$tracker.tick();
	}

	@Override
	public ChunkDebugTracker chunkdebug$getTracker() {
		return this.chunkdebug$tracker;
	}
}
