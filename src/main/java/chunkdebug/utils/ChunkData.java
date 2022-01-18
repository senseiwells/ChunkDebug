package chunkdebug.utils;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.util.math.ChunkPos;

public class ChunkData {
	private final ChunkPos chunkPos;
	private final ChunkHolder.LevelType levelType;
	private final byte ticketCode;

	public ChunkData(ChunkPos chunkPos, ChunkHolder.LevelType levelType, ChunkTicketType<?> ticketType) {
		this.chunkPos = chunkPos;
		this.levelType = levelType;
		this.ticketCode = getTicketCode(ticketType);
	}

	public boolean isLevelType(ChunkHolder.LevelType other) {
		return this.levelType == other;
	}

	public long getLongPos() {
		return this.chunkPos.toLong();
	}

	public byte getLevelByte() {
		return (byte) this.levelType.ordinal();
	}

	public byte getTicketByte() {
		return this.ticketCode;
	}

	private static byte getTicketCode(ChunkTicketType<?> chunkTicketType) {
		if (chunkTicketType == null) {
			return 0;
		}
		return (byte) switch (chunkTicketType.toString()) {
			default -> 0;
			case "start" -> 1;
			case "dragon" -> 2;
			case "player" -> 3;
			case "forced" -> 4;
			case "light" -> 5;
			case "portal" -> 6;
			case "post_teleport" -> 7;
			case "chonk" -> 8;
			case "unknown" -> 9;
		};
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