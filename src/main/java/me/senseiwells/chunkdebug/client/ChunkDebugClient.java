package me.senseiwells.chunkdebug.client;

import me.senseiwells.chunkdebug.ChunkDebug;
import me.senseiwells.chunkdebug.client.gui.ChunkDebugScreen;
import me.senseiwells.chunkdebug.common.network.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Supplier;

public class ChunkDebugClient implements ClientModInitializer {
	private static ChunkDebugClient instance;

	public final KeyMapping keybind = new KeyMapping("chunk-debug.key", GLFW.GLFW_KEY_F6, "key.categories.misc");

	@Nullable
	private ChunkDebugScreen screen;

	public static ChunkDebugClient getInstance() {
		return instance;
	}

	@Override
	public void onInitializeClient() {
		instance = this;

		KeyBindingHelper.registerKeyBinding(this.keybind);

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

		ClientPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE, this::handleHello);
		ClientPlayNetworking.registerGlobalReceiver(ByePayload.TYPE, this::handleBye);
		ClientPlayNetworking.registerGlobalReceiver(ChunkDataPayload.TYPE, this::handleChunkData);
		ClientPlayNetworking.registerGlobalReceiver(ChunkUnloadPayload.TYPE, this::handleChunkUnload);
	}

	public void startWatching(ResourceKey<Level> dimension) {
		this.trySendPayload(() -> new StartWatchingPayload(List.of(dimension)));
	}

	public void stopWatching() {
		this.trySendPayload(StopWatchingPayload::new);
	}

	@ApiStatus.Internal
	public void onGuiRender(GuiGraphics graphics, @SuppressWarnings("unused") DeltaTracker tracker) {
		if (this.screen != null) {
			this.screen.renderMinimap(graphics);
		}
	}

	@ApiStatus.Internal
	public void onGuiResize(Minecraft minecraft, int width, int height) {
		if (this.screen != null) {
			this.screen.resize(minecraft, width, height);
		}
	}

	private void onClientTick(Minecraft minecraft) {
		if (this.screen != null) {
			this.screen.clientTick();
		}
		if (this.keybind.consumeClick() && minecraft.screen == null) {
			if (this.screen != null) {
				minecraft.setScreen(this.screen);
			} else {
				minecraft.gui.getChat().addMessage(
					Component.translatable("chunk-debug.screen.unavailable").withStyle(ChatFormatting.RED)
				);
			}
		}
	}

	private void handleHello(HelloPayload payload, ClientPlayNetworking.Context context) {
		if (payload.version() == ChunkDebug.PROTOCOL_VERSION) {
			this.screen = new ChunkDebugScreen();
			ChunkDebug.LOGGER.info("ChunkDebug connection successful");
		} else if (payload.version() < ChunkDebug.PROTOCOL_VERSION) {
			ChunkDebug.LOGGER.info("ChunkDebug failed to connect, server is out of date!");
		} else {
			ChunkDebug.LOGGER.info("ChunkDebug failed to connect, client is out of date!");
		}
	}

	private void handleBye(ByePayload payload, ClientPlayNetworking.Context context) {
		Minecraft minecraft = context.client();
		if (minecraft.screen == this.screen) {
			minecraft.setScreen(null);
		}
		this.screen = null;
	}

	private void handleChunkData(ChunkDataPayload payload, ClientPlayNetworking.Context context) {
		if (this.screen != null) {
			this.screen.updateChunks(payload.dimension(), payload.chunks());
		}
	}

	private void handleChunkUnload(ChunkUnloadPayload payload, ClientPlayNetworking.Context context) {
		if (this.screen != null) {
			this.screen.unloadChunks(payload.dimension(), payload.positions());
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
