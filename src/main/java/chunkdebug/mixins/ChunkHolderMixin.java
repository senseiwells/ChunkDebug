package chunkdebug.mixins;

import chunkdebug.ChunkDebugServer;
import chunkdebug.utils.ChunkData;
import chunkdebug.utils.IChunkTicketManager;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
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
	@Shadow
	private int level;
	@Shadow
	@Final
	ChunkPos pos;

	@Shadow
	public static ChunkHolder.LevelType getLevelType(int distance) {
		throw new AssertionError();
	}

	@Shadow public abstract @Nullable Chunk getCurrentChunk();

	@Inject(method = "tick", at = @At("RETURN"))
	private void onTick(ThreadedAnvilChunkStorage chunkStorage, Executor executor, CallbackInfo ci) {
		ChunkHolder.LevelType levelType = getLevelType(this.level);
		ServerWorld world = ((ThreadedAnvilChunkStorageAccessor) chunkStorage).getWorld();
		ThreadedAnvilChunkStorage.TicketManager ticketManager = ((ThreadedAnvilChunkStorageAccessor) chunkStorage).getTicketManager();
		ChunkTicketType<?> ticketType = ((IChunkTicketManager) ticketManager).getTicketType(this.pos.toLong());
		Chunk chunk = this.getCurrentChunk();
		ChunkStatus status = chunk == null ? ChunkStatus.EMPTY : chunk.getStatus();
		ChunkDebugServer.chunkNetHandler.updateChunkMap(world, new ChunkData(this.pos, levelType, status, ticketType));
	}
}
