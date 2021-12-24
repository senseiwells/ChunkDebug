package chunkdebug.mixins;

import chunkdebug.utils.IChunkTicketManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.util.collection.SortedArraySet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkTicketManager.class)
public class ChunkTicketManagerMixin implements IChunkTicketManager {
	@Shadow
	@Final
	private Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>> ticketsByPosition;

	@Override
	public ChunkTicketType<?> getTicketType(long chunkPos) {
		SortedArraySet<ChunkTicket<?>> chunkTicket = this.ticketsByPosition.get(chunkPos);
		if (chunkTicket == null) {
			return null;
		}
		SortedArraySet<ChunkTicket<?>> ticketTypeSet = this.ticketsByPosition.get(chunkPos);
		// ChunkDebugServer.LOGGER.info(Arrays.toString(ticketTypeSet.toArray()));
		return ticketTypeSet.first().getType();
	}
}
