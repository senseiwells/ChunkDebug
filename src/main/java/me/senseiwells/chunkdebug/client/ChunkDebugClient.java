package me.senseiwells.chunkdebug.client;

import com.mojang.brigadier.Command;
import me.senseiwells.chunkdebug.ChunkDebug;
import me.senseiwells.chunkdebug.client.gui.ChunkDebugScreen;
import me.senseiwells.chunkdebug.common.network.ChunkDataPayload;
import me.senseiwells.chunkdebug.common.network.HelloPayload;
import me.senseiwells.chunkdebug.common.network.StartWatchingPayload;
import me.senseiwells.chunkdebug.common.network.StopWatchingPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class ChunkDebugClient implements ClientModInitializer {
	private static ChunkDebugClient instance;

	@Nullable
	private ChunkDebugScreen screen;

	public static ChunkDebugClient getInstance() {
		return instance;
	}

	@Override
	public void onInitializeClient() {
		instance = this;

		// TODO: For testing purposes
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
			dispatcher.register(ClientCommandManager.literal("chunk-debug").executes(c -> {
				c.getSource().getClient().tell(() -> {
					c.getSource().getClient().setScreen(this.screen);
				});
				return Command.SINGLE_SUCCESS;
			}));
		});

		ClientConfigurationNetworking.registerGlobalReceiver(HelloPayload.TYPE, this::handleHello);
		ClientPlayNetworking.registerGlobalReceiver(ChunkDataPayload.TYPE, this::handleChunkData);
	}

	public void startWatching(ResourceKey<Level> dimension) {
		this.trySendPayload(() -> new StartWatchingPayload(List.of(dimension)));
	}

	public void stopWatching() {
		this.trySendPayload(StopWatchingPayload::new);
	}

	public void onGuiRender(GuiGraphics graphics, DeltaTracker tracker) {
		if (this.screen != null) {
			this.screen.renderMinimap(graphics);
		}
	}

	public void onGuiResize(Minecraft minecraft, int width, int height) {
		if (this.screen != null) {
			this.screen.resize(minecraft, width, height);
		}
	}

	private void handleHello(HelloPayload payload, ClientConfigurationNetworking.Context context) {
		if (payload.version() == ChunkDebug.PROTOCOL_VERSION) {
			this.screen = new ChunkDebugScreen();
		}
	}

	private void handleChunkData(ChunkDataPayload payload, ClientPlayNetworking.Context context) {
		if (this.screen != null) {
			this.screen.updateChunks(payload.dimension(), payload.chunks());
		}
	}

	private void trySendPayload(Supplier<CustomPacketPayload> supplier) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientPacketListener listener = minecraft.getConnection();
		if (listener != null) {
			listener.send(new ServerboundCustomPayloadPacket(supplier.get()));
		}
	}
}
