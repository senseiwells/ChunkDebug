package chunkdebug.utils;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;

import java.util.Set;

public class ChunkMapSerializer {
	public static NbtCompound serialize(ServerWorld world, Set<ChunkData> chunkDataSet) {
		NbtList list = new NbtList();
		for (ChunkData chunkData : chunkDataSet) {
			NbtCompound innerCompound = new NbtCompound();
			innerCompound.putInt("x", chunkData.chunkPos.x);
			innerCompound.putInt("z", chunkData.chunkPos.z);
			innerCompound.putInt("t", chunkData.levelType.ordinal());
			innerCompound.putInt("l", chunkData.ticketCode);
			list.add(innerCompound);
		}
		NbtCompound baseCompound = new NbtCompound();
		baseCompound.put("chunks", list);
		baseCompound.putString("world", world.getRegistryKey().getValue().getPath());
		return baseCompound;
	}

	public static int getTicketCode(ChunkTicketType<?> chunkTicketType) {
		if (chunkTicketType == null) {
			return 0;
		}
		return switch (chunkTicketType.toString()) {
			default -> 0;
			case "start" -> 1;
			case "dragon" -> 2;
			case "player" -> 3;
			case "forced" -> 4;
			case "light" -> 5;
			case "portal" -> 6;
			case "post_teleport" -> 7;
			case "unknown" -> 8;
		};
	}
}