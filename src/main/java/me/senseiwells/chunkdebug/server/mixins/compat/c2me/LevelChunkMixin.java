package me.senseiwells.chunkdebug.server.mixins.compat.c2me;

import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin extends ChunkAccess {
    @Shadow @Final Level level;

    @NotNull @Shadow public abstract ChunkStatus getPersistedStatus();

    public LevelChunkMixin(
		Level level,
		ChunkPos pos,
		UpgradeData data,
		long inhabitedTime,
		LevelChunkSection[] sections,
		@Nullable BlendingData blendingData
    ) {
		super(pos, data, level, level.palettedContainerFactory(), inhabitedTime, sections, blendingData);
    }

	@Inject(
		method = "runPostLoad",
		at = @At("TAIL")
	)
	private void onPostLoad(CallbackInfo ci) {
		if (this.level instanceof ChunkDebugTrackerHolder holder) {
			ChunkDebugTracker tracker = holder.chunkdebug$getTracker();
			tracker.updateStage(this.chunkPos.toLong(), this.getPersistedStatus());
		}
	}
}
