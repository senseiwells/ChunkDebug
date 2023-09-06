package chunkdebug;

import chunkdebug.feature.ChunkServerNetworkHandler;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkDebugServer implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("ChunkDebug");
	public static final ChunkServerNetworkHandler chunkNetHandler = new ChunkServerNetworkHandler();

	@Override
	public void onInitialize() {

	}
}
