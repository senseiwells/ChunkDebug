package chunkdebug.mixins;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ThreadedAnvilChunkStorage.class)
public interface ThreadedAnvilChunkStorageAccessor {
	@Accessor("world")
	ServerWorld getWorld();

	@Accessor("ticketManager")
	ThreadedAnvilChunkStorage.TicketManager getTicketManager();

	@Accessor("chunkHolders")
	Long2ObjectLinkedOpenHashMap<ChunkHolder> getChunkHolderMap();
}
