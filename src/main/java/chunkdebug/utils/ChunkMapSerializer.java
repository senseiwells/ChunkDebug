package chunkdebug.utils;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

import java.util.Map;

public class ChunkMapSerializer {
	public static NbtCompound serialize(ServerWorld world, Map<ChunkPos, ChunkHolder.LevelType> chunkMap) {
		NbtList list = new NbtList();
		for (Map.Entry<ChunkPos, ChunkHolder.LevelType> entry : chunkMap.entrySet()) {
			NbtCompound innerCompound = new NbtCompound();
			ChunkPos chunkPos = entry.getKey();
			ChunkHolder.LevelType levelType = entry.getValue();
			innerCompound.putInt("x", chunkPos.x);
			innerCompound.putInt("z", chunkPos.z);
			innerCompound.putInt("type", levelType.ordinal());
			list.add(innerCompound);
		}
		NbtCompound baseCompound = new NbtCompound();
		baseCompound.put("chunks", list);
		baseCompound.putString("world", world.getRegistryKey().getValue().getPath());
		return baseCompound;
	}
}
