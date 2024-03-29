package chunkdebug.utils;

import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;

public class ChunkData {
	private final ChunkPos chunkPos;
	private final ChunkLevelType levelType;
	private final byte statusCode;
	private final byte ticketCode;

	public ChunkData(ChunkPos chunkPos, ChunkLevelType levelType, byte status, byte ticketCode) {
		this.chunkPos = chunkPos;
		this.levelType = levelType;
		this.statusCode = status;
		this.ticketCode = ticketCode;
	}

	public ChunkData(ChunkPos chunkPos, ChunkLevelType levelType, ChunkStatus status, ChunkTicketType<?> ticketType) {
		this(chunkPos, levelType, (byte) status.getIndex(), getTicketCode(ticketType));
	}

	public boolean isLevelType(ChunkLevelType other) {
		return this.levelType == other;
	}

	public ChunkPos getChunkPos() {
		return this.chunkPos;
	}

	public long getLongPos() {
		return this.chunkPos.toLong();
	}

	public ChunkLevelType getLevelType() {
		return this.levelType;
	}

	public byte getLevelByte() {
		return (byte) this.levelType.ordinal();
	}

	public byte getStatusByte() {
		return this.statusCode;
	}

	public byte getTicketByte() {
		return this.ticketCode;
	}

	public static byte getTicketCode(ChunkTicketType<?> chunkTicketType) {
		if (chunkTicketType != null) {
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
				// case "unknown" -> 9;
			};
		}
		return 0;
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