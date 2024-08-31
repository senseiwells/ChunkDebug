package me.senseiwells.chunkdebug.client.gui;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.senseiwells.chunkdebug.client.ChunkDebugClient;
import me.senseiwells.chunkdebug.common.utils.ChunkData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.Map;

import static me.senseiwells.chunkdebug.client.utils.RenderUtils.renderOutline;

public class ChunkDebugScreen extends Screen {
	private static final int SELECTING_OUTLINE_COLOR = 0xAAAA0000;
	private static final int SELECTED_OUTLINE_COLOR = 0xAAFF0000;

	private static final float MINIMAP_SCALE = 0.5F;

	private final Map<ResourceKey<Level>, DimensionState> states = new Object2ObjectOpenHashMap<>();
	private ResourceKey<Level> dimension = Level.OVERWORLD;
	private Minimap minimap = Minimap.FOLLOW;

	private boolean initialized = false;

	public ChunkDebugScreen() {
		super(Component.translatable("chunk-debug.screen.title"));
	}

	public void updateChunks(ResourceKey<Level> dimension, Collection<ChunkData> chunks) {
		DimensionState state = this.state(dimension);
		for (ChunkData chunk : chunks) {
			state.chunks.put(chunk.position().toLong(), chunk);
		}
	}

	@Override
	protected void init() {
		super.init();
		this.initialized = true;
		this.initializeDimension(this.dimension);
	}

	@Override
	public void added() {
		ChunkDebugClient.getInstance().startWatching(this.dimension);
	}

	@Override
	public void removed() {
		if (this.minimap == Minimap.NONE) {
			ChunkDebugClient.getInstance().stopWatching();
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
		super.render(graphics, mouseX, mouseY, partial);

		DimensionState state = this.state();

		graphics.pose().pushPose();
		graphics.pose().translate(state.offsetX, state.offsetY, 0.0);
		graphics.pose().scale(state.scale, state.scale, 0.0F);

		this.renderChunkDebugMap(graphics, state);

		if (state.first != null) {
			ChunkSelection selection = new ChunkSelection(state.first, this.convertScreenToChunkPos(mouseX, mouseY));
			this.renderChunkSelection(graphics, selection, SELECTING_OUTLINE_COLOR);
		}

		graphics.pose().popPose();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			DimensionState state = this.state();
			state.first = this.convertScreenToChunkPos(mouseX, mouseY);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		DimensionState state = this.state();
		if (state.first != null && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			ChunkSelection selection = new ChunkSelection(state.first, this.convertScreenToChunkPos(mouseX, mouseY));
			state.first = null;
			if (selection.equals(state.selection)) {
				state.selection = null;
			} else {
				state.selection = selection;
			}
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		DimensionState state = this.state();
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			state.offsetX += dragX;
			state.offsetY += dragY;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		DimensionState state = this.state();
		double currentX = (mouseX - state.offsetX) / state.scale;
		double currentY = (mouseY - state.offsetY) / state.scale;

		state.scale = Mth.clamp(state.scale + (float) scrollY, 1.0F, 64.0F);
		state.offsetX = mouseX - currentX * state.scale;
		state.offsetY = mouseY - currentY * state.scale;
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		if (!this.initialized) {
			return;
		}

		DimensionState state = this.state();

		double oldCenterX = this.width / 2.0;
		double oldCenterY = this.height / 2.0;

		double currentX = (oldCenterX - state.offsetX) / state.scale;
		double currentY = (oldCenterY - state.offsetY) / state.scale;

		super.resize(minecraft, width, height);

		double newCenterX = width / 2.0;
		double newCenterY = height / 2.0;
		state.offsetX = newCenterX - currentX * state.scale;
		state.offsetY = newCenterY - currentY * state.scale;
	}

	public void renderMinimap(GuiGraphics graphics) {
		if (this.minimap == Minimap.NONE || this.minecraft == null || this.minecraft.player == null) {
			return;
		}
		int size = 100;
		int padding = 10;

		int minX = this.width - size - padding;
		int minY = padding + 1 - 1;
		int maxX = this.width - padding;
		int maxY = size + padding;

		graphics.pose().pushPose();
		graphics.enableScissor(minX, minY, maxX, maxY);
		graphics.pose().translate(minX + size / 2.0, minY + size / 2.0, 0.0F);

		DimensionState state;
		if (this.minimap == Minimap.STATIC) {
			state = this.state();
			double offsetX = (this.width / 2.0 - state.offsetX) / state.scale;
			double offsetY = (this.height / 2.0 - state.offsetY) / state.scale;

			graphics.pose().scale(state.scale * MINIMAP_SCALE, state.scale * MINIMAP_SCALE, 0.0F);
			graphics.pose().translate(-offsetX, -offsetY, 0.0F);
		} else {
			LocalPlayer player = this.minecraft.player;
			state = this.state(player.level().dimension());
			ChunkPos pos = player.chunkPosition();

			graphics.pose().scale(state.scale * MINIMAP_SCALE, state.scale * MINIMAP_SCALE, 0.0F);
			graphics.pose().translate(-pos.x - 0.5, -pos.z - 0.5, 0.0F);
		}

		this.renderChunkDebugMap(graphics, state);

		graphics.disableScissor();
		graphics.pose().popPose();
	}

	private void renderChunkDebugMap(GuiGraphics graphics, DimensionState state) {
		for (ChunkData data : state.chunks.values()) {
			ChunkPos pos = data.position();
			graphics.fill(pos.x, pos.z, pos.x + 1, pos.z + 1, this.approximateChunkColor(data));
		}

		if (state.selection != null) {
			this.renderChunkSelection(graphics, state.selection, SELECTED_OUTLINE_COLOR);
		}
	}

	private void renderChunkSelection(GuiGraphics graphics, ChunkSelection selection, int color) {
		renderOutline(graphics, selection.minX, selection.minZ, selection.sizeX(), selection.sizeZ(), 0.3F, color);
	}

	private void initializeDimension(ResourceKey<Level> dimension) {
		DimensionState state = this.state(dimension);
		if (!state.centered) {
			ChunkPos center = ChunkPos.ZERO;
			if (this.minecraft != null) {
				LocalPlayer player = this.minecraft.player;
				if (player != null && player.level().dimension() == dimension) {
					center = player.chunkPosition();
				}
			}
			state.offsetX = this.width / 2.0 - center.x * state.scale;
			state.offsetY = this.height / 2.0 - center.z * state.scale;
			state.centered = true;
		}
	}

	private ChunkPos convertScreenToChunkPos(double x, double y) {
		DimensionState state = this.state();
		double scaledX = (x - state.offsetX) / state.scale;
		double scaledY = (y - state.offsetY) / state.scale;
		return new ChunkPos(Mth.floor(scaledX), Mth.floor(scaledY));
	}

	private int approximateChunkColor(ChunkData data) {
		int color = switch (data.status()) {
			case INACCESSIBLE -> 0xFF404040;
			case FULL -> 0xFF4FC3F7;
			case BLOCK_TICKING -> 0xFFFFA219;
			case ENTITY_TICKING -> 0xFF198C19;
		};
		if ((data.position().x + data.position().z) % 2 == 0) {
			return FastColor.ARGB32.lerp(0.12F, color, 0xFFFFFFFF);
		}
		return color;
	}

	private DimensionState state() {
		return this.state(this.dimension);
	}

	private DimensionState state(ResourceKey<Level> dimension) {
		return this.states.computeIfAbsent(dimension, d -> new DimensionState());
	}

	private enum Minimap {
		STATIC, FOLLOW, NONE
	}

	private static class DimensionState {
		private final Long2ObjectMap<ChunkData> chunks = new Long2ObjectOpenHashMap<>();

		private ChunkSelection selection;
		private ChunkPos first;

		private float scale = 1.0F;

		private double offsetX = 0.0;
		private double offsetY = 0.0;

		boolean centered = false;
	}
}
