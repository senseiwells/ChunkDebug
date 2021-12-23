package chunkdebug.mixins;

import chunkdebug.ChunkDebugServer;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin {

	@Shadow
	private int level;
	@Shadow
	@Final
	private ChunkPos pos;

	@Shadow
	public static ChunkHolder.LevelType getLevelType(int distance) {
		throw new AssertionError();
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(ThreadedAnvilChunkStorage chunkStorage, CallbackInfo ci) {
		ChunkHolder.LevelType levelType = getLevelType(this.level);
		ServerWorld world = ((ThreadedAnvilChunkStorageAccessor) chunkStorage).getWorld();
		ChunkDebugServer.chunkNetHandler.updateChunkMap(world, this.pos, levelType);
	}
}
