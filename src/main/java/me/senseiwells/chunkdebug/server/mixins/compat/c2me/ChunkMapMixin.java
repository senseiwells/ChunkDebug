package me.senseiwells.chunkdebug.server.mixins.compat.c2me;

import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.c2me.rewrites.chunksystem.common.TheChunkSystem;
import com.ishland.c2me.rewrites.chunksystem.common.ducks.IChunkSystemAccess;
import me.senseiwells.chunkdebug.server.holder.ChunkHolderSupplier;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;

@Mixin(ChunkMap.class)
public class ChunkMapMixin implements ChunkHolderSupplier {
	@Override
	@SuppressWarnings("unchecked")
	public void chunkdebug$forEachChunkHolder(@NonNull Consumer<ChunkHolder> consumer) {
		TheChunkSystem system = ((IChunkSystemAccess) this).c2me$getTheChunkSystem();
		var accessor = (StatusAdvancingSchedulerAccessor<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface>) system;
		StampedLock lock = accessor.getItemsLock();
		long stamp = lock.readLock();
		try {
			for (var item : accessor.getItems().values()) {
				ChunkHolder holder = item.getUserData().get();
				if (holder != null) {
					consumer.accept(holder);
				}
			}
		} finally {
			lock.unlockRead(stamp);
		}
	}
}
