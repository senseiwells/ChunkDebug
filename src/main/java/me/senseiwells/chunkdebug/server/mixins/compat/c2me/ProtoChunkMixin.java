package me.senseiwells.chunkdebug.server.mixins.compat.c2me;

import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProtoChunk.class)
public abstract class ProtoChunkMixin extends ChunkAccess {
	public ProtoChunkMixin(
		ChunkPos chunkPos,
		UpgradeData upgradeData,
		LevelHeightAccessor levelHeightAccessor,
		Registry<Biome> biomeRegistry,
		long inhabitedTime,
		@Nullable LevelChunkSection[] sections,
		@Nullable BlendingData blendingData
	) {
		super(chunkPos, upgradeData, levelHeightAccessor, biomeRegistry, inhabitedTime, sections, blendingData);
	}

	@Inject(
		method = "setPersistedStatus",
		at = @At("TAIL")
	)
	private void onSetPersistedStatus(ChunkStatus status, CallbackInfo ci) {
		if (this.levelHeightAccessor instanceof ChunkDebugTrackerHolder holder) {
			ChunkDebugTracker tracker = holder.chunkdebug$getTracker();
			tracker.updateStage(this.chunkPos.toLong(), status);
		}
	}
}
