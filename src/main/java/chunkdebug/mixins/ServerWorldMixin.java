package chunkdebug.mixins;

import chunkdebug.ChunkDebugServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

//#if MC >= 12000
import net.minecraft.util.math.random.RandomSequencesState;
//#endif

//#if MC >= 11903
import net.minecraft.registry.RegistryKey;
//#else
//$$import net.minecraft.util.registry.RegistryKey;
//#endif

//#if MC >= 11900
import net.minecraft.world.dimension.DimensionOptions;
//#elseif MC >= 11800
//$$import net.minecraft.util.registry.RegistryEntry;
//#endif

//#if MC < 11900
//$$import net.minecraft.util.registry.RegistryKey;
//$$import net.minecraft.world.gen.chunk.ChunkGenerator;
//#endif

//#if MC < 11800
//$$import net.minecraft.world.dimension.DimensionType;
//#endif

import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.spawner.Spawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {
	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;", remap = false))
	private void onCreateServerWorld(
		MinecraftServer server,
		Executor workerExecutor,
		LevelStorage.Session session,
		ServerWorldProperties properties,
		RegistryKey<World> worldKey,
		//#if MC >= 11900
		DimensionOptions dimensionOptions,
		//#elseif MC >= 11800
		//$$RegistryEntry<?> registryEntry,
		//#else
		//$$DimensionType dimensionType,
		//#endif
		WorldGenerationProgressListener worldGenerationProgressListener,
		//#if MC < 11900
		//$$ChunkGenerator chunkGenerator,
		//#endif
		boolean debugWorld,
		long seed,
		List<Spawner> spawners,
		boolean shouldTickTime,
		//#if MC >= 12000
		RandomSequencesState randomSequencesState,
		//#endif
		CallbackInfo ci
	) {
		ChunkDebugServer.chunkNetHandler.addWorld((ServerWorld) (Object) this);
	}
}



