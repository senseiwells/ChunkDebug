package me.senseiwells.chunkdebug.client.mixins;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelLoadingScreen.class)
public interface LevelLoadingScreenAccessor {
	@Accessor("COLORS")
	static Object2IntMap<ChunkStatus> getStageColorMap() {
		throw new AssertionError();
	}
}
