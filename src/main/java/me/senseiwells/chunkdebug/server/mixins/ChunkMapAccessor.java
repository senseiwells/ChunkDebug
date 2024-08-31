package me.senseiwells.chunkdebug.server.mixins;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
	@Accessor("level")
	ServerLevel getLevel();

	@Accessor("distanceManager")
	ChunkMap.DistanceManager getDistanceManager();

	@Accessor("visibleChunkMap")
	Long2ObjectLinkedOpenHashMap<ChunkHolder> getChunkHolderMap();
}
