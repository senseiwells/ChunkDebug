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

//#if MC < 12000
//$$import net.minecraft.server.world.ChunkHolder.LevelType;
//#endif

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin {
	@Shadow @Final
	//#if MC >= 11700
	ChunkPos pos;
	//#else
	//$$private ChunkPos pos;
	//#endif

	//#if MC >= 12000
	@Shadow public abstract ChunkLevelType getLevelType();
	//#else
	//$$@Shadow private int level;
	//#endif

	@Shadow public abstract @Nullable Chunk getCurrentChunk();

	@Inject(method = "tick", at = @At("RETURN"))
	//#if MC >= 11700
	private void onTick(ThreadedAnvilChunkStorage chunkStorage, Executor executor, CallbackInfo ci) {
		//#else
		//$$private void onTick(ThreadedAnvilChunkStorage chunkStorage, CallbackInfo ci) {
		//#endif

		//#if MC >= 12000
		ChunkLevelType levelType = this.getLevelType();
		//#else
		//$$LevelType levelType = ChunkHolder.getLevelType(this.level);
		//#endif
		ServerWorld world = ((ThreadedAnvilChunkStorageAccessor) chunkStorage).getWorld();
		ThreadedAnvilChunkStorage.TicketManager ticketManager = ((ThreadedAnvilChunkStorageAccessor) chunkStorage).getTicketManager();
		long posLong = this.pos.toLong();
		ChunkTicketType<?> ticketType = ((IChunkTicketManager) ticketManager).getTicketType(posLong);
		//#if MC >= 11800
		if (levelType == ChunkLevelType.ENTITY_TICKING && !ticketManager.shouldTickEntities(posLong)) {
			levelType = ChunkLevelType.BLOCK_TICKING;
		}
		//#endif
		Chunk chunk = this.getCurrentChunk();
		ChunkStatus status = chunk == null ? ChunkStatus.EMPTY : chunk.getStatus();
		ChunkDebugServer.chunkNetHandler.updateChunkMap(world, new ChunkData(this.pos, levelType, status, ticketType));
	}
}
