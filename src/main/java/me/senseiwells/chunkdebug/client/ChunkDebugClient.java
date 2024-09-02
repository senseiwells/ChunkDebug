package me.senseiwells.chunkdebug.client;

import com.mojang.brigadier.Command;
import me.senseiwells.chunkdebug.ChunkDebug;
import me.senseiwells.chunkdebug.client.gui.ChunkDebugScreen;
import me.senseiwells.chunkdebug.common.network.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
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

		ClientConfigurationNetworking.registerGlobalReceiver(HelloPayload.TYPE, this::handleHello);
		ClientPlayNetworking.registerGlobalReceiver(ChunkDataPayload.TYPE, this::handleChunkData);
		ClientPlayNetworking.registerGlobalReceiver(ChunkUnloadPayload.TYPE, this::handleChunkUnload);
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

	private void onClientTick(Minecraft minecraft) {
		if (this.keybind.consumeClick() && minecraft.screen == null) {
			minecraft.setScreen(this.screen);
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
