package chunkdebug;

import chunkdebug.feature.ChunkServerNetworkHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkDebugServer implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("ChunkDebug");
	public static final ChunkServerNetworkHandler chunkNetHandler = new ChunkServerNetworkHandler();

	@Override
	public void onInitialize() {
		ServerTickEvents.START_SERVER_TICK.register(s -> chunkNetHandler.tick());
		ServerPlayConnectionEvents.JOIN.register((handler, r, s) -> chunkNetHandler.sayHello(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> chunkNetHandler.removeHandler(handler));
		ServerWorldEvents.LOAD.register((s, w) -> chunkNetHandler.addWorld(w));

		ServerPlayNetworking.registerGlobalReceiver(
			ChunkServerNetworkHandler.ESSENTIAL_CHANNEL,
			(server, player, h, buf, r) -> server.execute(() -> chunkNetHandler.handlePacket(buf, player))
		);
	}
}
