package chunkdebug.mixins;

import chunkdebug.ChunkDebugServer;
import chunkdebug.utils.ChunkData;
import chunkdebug.utils.IChunkTicketManager;
import net.minecraft.server.world.*;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin {
	@Shadow @Final ChunkPos pos;
	@Shadow public abstract ChunkLevelType getLevelType();

	@Shadow public abstract @Nullable Chunk getCurrentChunk();

	@Inject(method = "updateFutures", at = @At("RETURN"))
	private void onTick(ThreadedAnvilChunkStorage chunkStorage, Executor executor, CallbackInfo ci) {
		ChunkLevelType levelType = this.getLevelType();
		ServerWorld world = ((ThreadedAnvilChunkStorageAccessor) chunkStorage).getWorld();
		ThreadedAnvilChunkStorage.TicketManager ticketManager = ((ThreadedAnvilChunkStorageAccessor) chunkStorage).getTicketManager();
		long posLong = this.pos.toLong();
		ChunkTicketType<?> ticketType = ((IChunkTicketManager) ticketManager).chunkdebug$getTicketType(posLong);

		if (levelType == ChunkLevelType.ENTITY_TICKING && !ticketManager.shouldTickEntities(posLong)) {
			levelType = ChunkLevelType.BLOCK_TICKING;
		}

		Chunk chunk = this.getCurrentChunk();
		ChunkStatus status = chunk == null ? ChunkStatus.EMPTY : chunk.getStatus();
		ChunkDebugServer.chunkNetHandler.updateChunkMap(world, new ChunkData(this.pos, levelType, status, ticketType));
	}
}
