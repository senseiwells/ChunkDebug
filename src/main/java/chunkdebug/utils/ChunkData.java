package chunkdebug.utils;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.util.math.ChunkPos;

public class ChunkData {
	public ChunkPos chunkPos;
	public ChunkHolder.LevelType levelType;
	public int ticketCode;

	public ChunkData(ChunkPos chunkPos, ChunkHolder.LevelType levelType, ChunkTicketType<?> ticketType) {
		this.chunkPos = chunkPos;
		this.levelType = levelType;
		this.ticketCode = ChunkMapSerializer.getTicketCode(ticketType);
	}

	@Override
	public int hashCode() {
		return this.chunkPos.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ChunkData chunkData) {
			return this.chunkPos.equals(chunkData.chunkPos);
		}
		return false;
	}
}