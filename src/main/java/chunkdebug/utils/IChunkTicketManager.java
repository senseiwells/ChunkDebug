package chunkdebug.utils;

import net.minecraft.server.world.ChunkTicketType;

public interface IChunkTicketManager {
	ChunkTicketType<?> chunkdebug$getTicketType(long chunkPos);
}
