package me.senseiwells.chunkdebug.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.senseiwells.chunkdebug.ChunkDebug;
import me.senseiwells.chunkdebug.client.config.ChunkDebugClientConfig;
import me.senseiwells.chunkdebug.client.gui.ChunkDebugMap;
import me.senseiwells.chunkdebug.client.gui.ChunkDebugScreen;
import me.senseiwells.chunkdebug.common.network.*;
import me.senseiwells.keybinds.api.InputKeys;
import me.senseiwells.keybinds.api.Keybind;
import me.senseiwells.keybinds.api.KeybindListener;
import me.senseiwells.keybinds.api.KeybindManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class ChunkDebugClient implements ClientModInitializer {
    @Nullable private static ChunkDebugClient instance;

	public final ChunkDebugClientConfig config = ChunkDebugClientConfig.read();
	public final Keybind keybind = KeybindManager.register(ChunkDebug.id("map"), InputKeys.of(InputConstants.KEY_F6));

	@Nullable private ChunkDebugMap map;

	public static ChunkDebugClient getInstance() {
		return Objects.requireNonNull(instance, "Tried to get ChunkDebugClient before it was initialized!");
	}

	@Override
	public void onInitializeClient() {
		instance = this;

		this.keybind.setKeys(this.config.chunkDebugMapKeys);
		KeybindManager.addToControlsScreen(KeyMapping.Category.MISC, this.keybind);
		this.keybind.addListener(KeybindListener.onSetKeys(keys -> {
			this.config.chunkDebugMapKeys = keys;
		}));

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(this::onClientStopping);

		ClientPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE, this::handleHello);
		ClientPlayNetworking.registerGlobalReceiver(ByePayload.TYPE, this::handleBye);
		ClientPlayNetworking.registerGlobalReceiver(ChunkDataPayload.TYPE, this::handleChunkData);
		ClientPlayNetworking.registerGlobalReceiver(ChunkUnloadPayload.TYPE, this::handleChunkUnload);

		HudElementRegistry.attachElementAfter(
			VanillaHudElements.TITLE_AND_SUBTITLE, ChunkDebug.id("minimap"), this::onGuiRender
		);
	}

	@Nullable
	public ChunkDebugScreen createChunkDebugScreen(@Nullable Screen parent) {
		if (this.map == null) {
			return null;
		}
		return new ChunkDebugScreen(this.map, parent);
	}

	public void startWatching(ResourceKey<Level> dimension) {
		this.trySendPayload(() -> new StartWatchingPayload(List.of(dimension)));
	}

	public void stopWatching() {
		this.trySendPayload(StopWatchingPayload::new);
	}

	public void refresh() {
		this.trySendPayload(() -> ChunkRefreshPayload.INSTANCE);
	}

	@ApiStatus.Internal
	public void onGuiRender(GuiGraphicsExtractor graphics, @SuppressWarnings("unused") DeltaTracker tracker) {
		if (this.map != null) {
			this.map.renderMinimap(graphics);
		}
	}

	@ApiStatus.Internal
	public void onGuiResize(int width, int height) {
		if (this.map != null) {
			this.map.resize(width, height);
		}
	}

	private void onClientTick(Minecraft minecraft) {
		if (this.map != null) {
			this.map.tick();
		}
		if (this.keybind.consumeClicks() != 0 && minecraft.gui.screen() == null) {
			ChunkDebugScreen screen = this.createChunkDebugScreen(null);
			if (screen == null) {
				minecraft.gui.hud.getChat().addClientSystemMessage(
					Component.translatable("chunk-debug.screen.unavailable").withStyle(ChatFormatting.RED)
				);
			} else {
				minecraft.gui.setScreen(screen);
			}
		}
	}

	private void onClientStopping(Minecraft minecraft) {
		this.setChunkMap(null);
		ChunkDebugClientConfig.write(this.config);
	}

	private void handleHello(HelloPayload payload, ClientPlayNetworking.Context context) {
		if (payload.version() == ChunkDebug.PROTOCOL_VERSION) {
			this.setChunkMap(new ChunkDebugMap(context.client(), this));
			ChunkDebug.LOGGER.info("ChunkDebug connection successful");
		} else if (payload.version() < ChunkDebug.PROTOCOL_VERSION) {
			ChunkDebug.LOGGER.info("ChunkDebug failed to connect, server is out of date!");
		} else {
			ChunkDebug.LOGGER.info("ChunkDebug failed to connect, client is out of date!");
		}
	}

	private void handleBye(ByePayload payload, ClientPlayNetworking.Context context) {
		Minecraft minecraft = context.client();
		if (minecraft.gui.screen() instanceof ChunkDebugScreen screen) {
			screen.onClose();
		}
		this.setChunkMap(null);
	}

	private void handleChunkData(ChunkDataPayload payload, ClientPlayNetworking.Context context) {
		if (this.map != null) {
			this.map.updateChunks(payload.dimension(), payload.chunks());
		}
	}

	private void handleChunkUnload(ChunkUnloadPayload payload, ClientPlayNetworking.Context context) {
		if (this.map != null) {
			this.map.unloadChunks(payload.dimension(), payload.positions());
		}
	}

	private void trySendPayload(Supplier<CustomPacketPayload> supplier) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientPacketListener listener = minecraft.getConnection();
		if (listener != null) {
			listener.send(new ServerboundCustomPayloadPacket(supplier.get()));
		}
	}

	private void setChunkMap(@Nullable ChunkDebugMap map) {
		if (this.map != null) {
			this.map.close();
		}
		this.map = map;
	}
}
