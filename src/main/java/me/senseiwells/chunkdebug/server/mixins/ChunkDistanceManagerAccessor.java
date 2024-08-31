package me.senseiwells.chunkdebug.server.mixins;

import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkMap.DistanceManager.class)
public interface ChunkDistanceManagerAccessor {
	@Accessor("field_17443")
	ChunkMap getChunkMap();
}
