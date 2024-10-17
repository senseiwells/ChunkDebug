package me.senseiwells.chunkdebug.client.gui;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import me.senseiwells.chunkdebug.client.ChunkDebugClient;
import me.senseiwells.chunkdebug.client.config.ChunkDebugClientConfig;
import me.senseiwells.chunkdebug.client.gui.widget.ArrowButton;
import me.senseiwells.chunkdebug.client.gui.widget.ArrowButton.Direction;
import me.senseiwells.chunkdebug.client.gui.widget.NamedButton;
import me.senseiwells.chunkdebug.client.gui.widget.IntegerEditbox;
import me.senseiwells.chunkdebug.client.gui.widget.ToggleButton;
import me.senseiwells.chunkdebug.client.utils.Bounds;
import me.senseiwells.chunkdebug.client.utils.RenderUtils;
import me.senseiwells.chunkdebug.common.utils.ChunkData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.Ticket;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static me.senseiwells.chunkdebug.client.utils.RenderUtils.*;

public class ChunkDebugScreen extends Screen {
	private static final int SELECTING_OUTLINE_COLOR = 0xAAAA0000;
	private static final int SELECTED_OUTLINE_COLOR = 0xAAFF0000;
	private static final int CLUSTER_OUTLINE_COLOR = HL;
	private static final int PLAYER_COLOR = 0xAAFFFF00;

	private static final float MINIMAP_SCALE = 0.5F;

	private static final int MENU_PADDING = 3;

	private final ChunkDebugClientConfig config;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final Map<ResourceKey<Level>, DimensionState> states = new Object2ObjectOpenHashMap<>();
	private final List<ResourceKey<Level>> dimensions = new ArrayList<>();
	private int dimensionWidth = 0;
	private int dimensionIndex = 0;

	private ChunkSelection clusterSelection;
	private int clusterTicks = 0;
	private int clusterIndex = 0;

	private ChunkPos center = ChunkPos.ZERO;

	private Minimap minimap = Minimap.NONE;
	private boolean draggingMinimap = false;

	private ToggleButton breakdown;
	private ToggleButton settings;

	private ArrowButton dimensionLeft;
	private ArrowButton dimensionRight;

	private ArrowButton minimapLeft;
	private ArrowButton minimapRight;

	private ArrowButton minimapCornerLeft;
	private ArrowButton minimapCornerRight;

	private NamedButton returnToPlayer;

	private ArrowButton clustersLeft;
	private ArrowButton clustersRight;

	private IntegerEditbox chunkPosX;
	private IntegerEditbox chunkPosZ;

	private ToggleButton showStages;
	private ToggleButton showTickets;
	private ToggleButton showMinimap;

	private IntegerEditbox chunkRetention;

	private boolean initialized = false;

	private int ticks = 0;

	public ChunkDebugScreen(ChunkDebugClientConfig config) {
		super(Component.translatable("chunk-debug.screen.title"));
		this.config = config;
	}

	public void updateChunks(ResourceKey<Level> dimension, Collection<ChunkData> chunks) {
		DimensionState state = this.state(dimension);
		for (ChunkData chunk : chunks) {
			state.add(chunk.position().toLong(), chunk);
		}
	}

	public void unloadChunks(ResourceKey<Level> dimension, long[] positions) {
		DimensionState state = this.state(dimension);
		for (long position : positions) {
			state.remove(position, this.ticks + this.chunkRetention.getIntValue());
		}
	}

	@Override
	protected void init() {
		super.init();
		this.initialized = true;

		this.breakdown = new ToggleButton(this.width - 20, this.height - 20, 15);
		this.breakdown.setTooltip(Tooltip.create(Component.translatable("chunk-debug.info.breakdown.toggle")));
		this.breakdown.setToggled(true);
		this.addRenderableWidget(this.breakdown);

		this.settings = new ToggleButton(5, this.height - 20, 15);
		this.settings.setTooltip(Tooltip.create(Component.translatable("chunk-debug.settings.toggle")));
		this.settings.setToggled(true);
		this.addRenderableWidget(this.settings);

		this.dimensionLeft = new ArrowButton(Direction.LEFT, 9, 0, 15, () -> this.incrementDimension(-1));
		this.addRenderableWidget(this.dimensionLeft);
		this.dimensionRight = new ArrowButton(Direction.RIGHT, 0, 0, 15, () -> this.incrementDimension(1));
		this.addRenderableWidget(this.dimensionRight);

		this.minimapLeft = new ArrowButton(Direction.LEFT, 0, 0, 15, () -> this.minimap = this.minimap.previous());
		this.addRenderableWidget(this.minimapLeft);
		this.minimapRight = new ArrowButton(Direction.RIGHT, 0, 0, 15, () -> this.minimap = this.minimap.next());
		this.addRenderableWidget(this.minimapRight);

		this.minimapCornerLeft = new ArrowButton(Direction.LEFT, 0, 0, 15, () -> {
			this.config.minimapCorner = this.config.minimapCorner.previous();
			this.config.minimapOffsetX = this.config.minimapOffsetY = 0;
		});
		this.addRenderableWidget(this.minimapCornerLeft);
		this.minimapCornerRight = new ArrowButton(Direction.RIGHT, 0, 0, 15, () -> {
			this.config.minimapCorner = this.config.minimapCorner.next();
			this.config.minimapOffsetX = this.config.minimapOffsetY = 0;
		});
		this.addRenderableWidget(this.minimapCornerRight);

		Component player = Component.translatable("chunk-debug.settings.return");
		this.returnToPlayer = new NamedButton(0, 0, 0, 15, player, () -> {
			if (this.minecraft != null && this.minecraft.player != null) {
				this.dimensionIndex = this.dimensions.indexOf(this.minecraft.player.level().dimension());
				this.setMapCenter(this.minecraft.player.chunkPosition());
			}
		});
		this.addRenderableWidget(this.returnToPlayer);

		this.clustersLeft = new ArrowButton(Direction.LEFT, 0, 0, 15, () -> this.jumpToCluster(-1));
		this.addRenderableWidget(this.clustersLeft);
		this.clustersRight = new ArrowButton(Direction.RIGHT, 0, 0, 15, () -> this.jumpToCluster(1));
		this.addRenderableWidget(this.clustersRight);

		this.chunkPosX = new IntegerEditbox(this.font, 40, 15, x -> this.setMapCenter(x, this.center.z));
		this.addRenderableWidget(this.chunkPosX);
		this.chunkPosZ = new IntegerEditbox(this.font, 40, 15, z -> this.setMapCenter(this.center.x, z));
		this.addRenderableWidget(this.chunkPosZ);

		this.showStages = new ToggleButton(0, 0, 15, b -> this.config.showStages = b);
		this.showStages.setToggled(this.config.showStages);
		this.addRenderableWidget(this.showStages);

		this.showTickets = new ToggleButton(0, 0, 15, b -> this.config.showTickets = b);
		this.showTickets.setToggled(this.config.showTickets);
		this.addRenderableWidget(this.showTickets);

		this.showMinimap = new ToggleButton(0, 0, 15, b -> this.config.showMinimap = b);
		this.showMinimap.setTooltip(Tooltip.create(Component.translatable("chunk-debug.settings.visibility.minimap.tooltip")));
		this.showMinimap.setToggled(this.config.showMinimap);
		this.addRenderableWidget(this.showMinimap);

		this.chunkRetention = new IntegerEditbox(this.font, 30, 15, i -> this.config.chunkRetention = i);
		this.chunkRetention.setTooltip(Tooltip.create(Component.translatable("chunk-debug.settings.visibility.unload.tooltip")));
		this.chunkRetention.setIntValue(this.config.chunkRetention);
		this.addRenderableWidget(this.chunkRetention);

		// Tick jump [<] [7832] [>]
	}

	@Override
	public void tick() {
		if (this.clusterTicks > 0) {
			this.clusterTicks--;
		} else {
			this.clusterSelection = null;
		}
	}

	public void clientTick() {
		int tick = ++this.ticks;
		for (DimensionState state : this.states.values()) {
			state.unloaded.removeAll(tick);
		}
	}

	@Override
	protected void repositionElements() {
		this.breakdown.setPosition(this.width - 20, this.height - 20);
		this.settings.setY(this.height - 20);
	}

	@Override
	public void added() {
		ChunkDebugClient.getInstance().startWatching(this.dimension());
	}

	@Override
	public void removed() {
		if (this.minimap == Minimap.NONE) {
			ChunkDebugClient.getInstance().stopWatching();
			this.states.clear();
		}
	}

	public void shutdown() {
		this.executor.shutdown();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
		this.renderBlurredBackground();

		DimensionState state = this.state();

		graphics.pose().pushPose();
		graphics.pose().translate(state.offsetX, state.offsetY, 0.0);
		graphics.pose().scale(state.scale, state.scale, 0.0F);

		this.renderChunkDebugMap(graphics, state);

		if (state.first != null) {
			ChunkSelection selection = new ChunkSelection(state.first, this.convertScreenToChunkPos(mouseX, mouseY));
			this.renderChunkSelection(graphics, selection, SELECTING_OUTLINE_COLOR);
		}
		if (this.clusterSelection != null) {
			this.renderChunkSelection(graphics, this.clusterSelection, 2.0F, CLUSTER_OUTLINE_COLOR);
		}

		graphics.pose().popPose();

		if (this.showMinimap.isToggled()) {
			this.renderMinimap(graphics);
		}

		this.renderChunkSelectionMenu(graphics, state);
		this.renderSettingsMenu(graphics);

		this.center = this.convertScreenToChunkPos(this.width / 2.0, this.height / 2.0);
		if (!this.chunkPosX.isFocused()) {
			this.chunkPosX.setIntValue(this.center.x);
		}
		if (!this.chunkPosZ.isFocused()) {
			this.chunkPosZ.setIntValue(this.center.z);
		}

		super.render(graphics, mouseX, mouseY, partial);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_F1) {
			boolean visible = this.settings.isToggled() || this.breakdown.isToggled();
			this.settings.setToggled(!visible);
			this.breakdown.setToggled(!visible);
			return true;
		}
		if (this.minecraft != null && ChunkDebugClient.getInstance().keybind.matches(keyCode, scanCode)) {
			this.onClose();
			return true;
		}
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		this.chunkPosX.setFocused(false);
		this.chunkPosZ.setFocused(false);
		this.chunkRetention.setFocused(false);
		if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			DimensionState state = this.state();
			state.first = this.convertScreenToChunkPos(mouseX, mouseY);
			return true;
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			if (this.showMinimap.isToggled() && this.getMinimapBounds().contains(mouseX, mouseY)) {
				this.draggingMinimap = true;
				return true;
			}
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
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			this.draggingMinimap = false;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		DimensionState state = this.state();
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			if (this.draggingMinimap) {
				this.config.minimapOffsetX += dragX;
				this.config.minimapOffsetY += dragY;
			} else {
				state.offsetX += dragX;
				state.offsetY += dragY;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.getMinimapBounds().contains(mouseX, mouseY)) {
			this.config.minimapSize = Mth.clamp(this.config.minimapSize + (int) scrollY, 20, 200);
			return true;
		}

		DimensionState state = this.state();
		double currentX = (mouseX - state.offsetX) / state.scale;
		double currentY = (mouseY - state.offsetY) / state.scale;

		state.scale = Mth.clamp(state.scale + (float) scrollY * 0.5F, 1.0F, 64.0F);
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

		Bounds bounds = this.getMinimapBounds();
		int minX = bounds.minX(), minY = bounds.minY(), maxX = bounds.maxX(), maxY = bounds.maxY();

		graphics.pose().pushPose();

		graphics.fill(minX - 3, minY - 3, maxX + 3, maxY + 3, HL_BG_LIGHT);
		graphics.fill(minX, minY, maxX, maxY, HL_BG_DARK);

		graphics.enableScissor(minX, minY, maxX, maxY);
		graphics.pose().translate(minX + this.config.minimapSize / 2.0, minY + this.config.minimapSize / 2.0, 0.0F);

		DimensionState state;
		if (this.minimap == Minimap.STATIC) {
			state = this.state();
			double offsetX = (this.width / 2.0 - state.offsetX) / state.scale;
			double offsetY = (this.height / 2.0 - state.offsetY) / state.scale;

			graphics.pose().scale(state.scale * MINIMAP_SCALE, state.scale * MINIMAP_SCALE, 0.0F);
			graphics.pose().translate(-offsetX, -offsetY, 0.0F);
		} else {
			LocalPlayer player = this.minecraft.player;
			ResourceKey<Level> dimension = player.level().dimension();
			state = this.state(dimension);
			ChunkPos pos = player.chunkPosition();

			graphics.pose().scale(state.scale * MINIMAP_SCALE, state.scale * MINIMAP_SCALE, 0.0F);
			graphics.pose().translate(-pos.x - 0.5, -pos.z - 0.5, 0.0F);
		}

		this.renderChunkDebugMap(graphics, state);

		graphics.disableScissor();
		graphics.pose().popPose();
	}

	private Bounds getMinimapBounds() {
		int padding = 10;

		int minX = this.config.minimapCorner.isLeft() ? padding : this.width - this.config.minimapSize - padding;
		int minY = this.config.minimapCorner.isTop() ? padding : this.height - this.config.minimapSize - padding;
		int maxX = minX + this.config.minimapSize;
		int maxY = minY + this.config.minimapSize;
		return new Bounds(minX, minY, maxX, maxY).offset((int) this.config.minimapOffsetX, (int) this.config.minimapOffsetY);
	}

	@SuppressWarnings("deprecation")
	private void renderChunkDebugMap(GuiGraphics graphics, DimensionState state) {
		for (ChunkData data : state.chunks.values()) {
			ChunkPos pos = data.position();
			int color = this.calculateChunkColor(data);
			graphics.fill(pos.x, pos.z, pos.x + 1, pos.z + 1, color);
		}

		int ticks = this.chunkRetention.getIntValue();
		if (ticks > 0) {
			for (Map.Entry<Integer, ChunkData> entry : state.unloaded.entries()) {
				float delta = (float) (entry.getKey() - this.ticks) / ticks;
				int alpha = ((byte) (delta * 255)) << 24 | 0xFFFFFF;
				ChunkData data = entry.getValue();
				ChunkPos pos = data.position();
				int color = this.calculateChunkColor(data) & alpha;
				graphics.fill(pos.x, pos.z, pos.x + 1, pos.z + 1, color);
			}
		}

		if (state.selection != null) {
			this.renderChunkSelection(graphics, state.selection, SELECTED_OUTLINE_COLOR);
		}

		if (this.minecraft != null && this.minecraft.player != null) {
			if (this.minecraft.player.level().dimension() == state.dimension) {
				this.renderPlayer(graphics, this.minecraft.player.chunkPosition());
			}
		}
	}

	private void renderSettingsMenu(GuiGraphics graphics) {
		RenderUtils.setVisible(
			this.settings.isToggled(),
			this.dimensionLeft, this.dimensionRight,
			this.minimapLeft, this.minimapRight,
			this.minimapCornerLeft, this.minimapCornerRight,
			this.clustersLeft, this.clustersRight,
			this.returnToPlayer, this.showMinimap,
			this.showStages, this.showTickets,
			this.chunkPosX, this.chunkPosZ,
			this.chunkRetention
		);
		if (!this.settings.isToggled()) {
			return;
		}

		graphics.pose().pushPose();
		Component title = Component.translatable("chunk-debug.settings").withColor(HL);
		Component player = Component.translatable("chunk-debug.settings.return");
		Component clusters = Component.translatable("chunk-debug.settings.clusters");
		Component stages = Component.translatable("chunk-debug.settings.visibility.stages");
		Component tickets = Component.translatable("chunk-debug.settings.visibility.tickets");
		Component minimap = Component.translatable("chunk-debug.settings.visibility.minimap");
		Component corner = Component.translatable("chunk-debug.settings.minimap.corner");
		Component fade = Component.translatable("chunk-debug.settings.visibility.unload");

		int padding = MENU_PADDING;
		int width = Math.max(this.dimensionWidth, RenderUtils.maxWidth(this.font, clusters, minimap)) + 2 * padding + 30;
		width = Math.max(width,  RenderUtils.maxWidth(this.font, title, player));
		width = Math.max(width, RenderUtils.maxWidth(this.font, stages, tickets, fade) + padding + 15);
		width += 4 * padding;
		int minX = padding + 1 - 1;
		int minY = padding + 1 - 1;
		int maxX = padding + width;
		int maxY = this.height - padding;
		int centerX = (maxX + minX) / 2;

		graphics.fill(minX, minY, maxX, maxY, HL_BG_LIGHT);
		graphics.drawString(this.font, title, minX + padding, minY + padding, 0xFFFFFFFF);

		int offsetY = minY + padding * 2 + this.font.lineHeight;
		int gap = padding + 15;

		Component mode = this.minimap.pretty();
		RenderUtils.options(graphics, this.font, minX, maxX, offsetY, padding, mode, this.minimapLeft, this.minimapRight);

		offsetY += gap + 3;
		Component dimension = Component.literal(this.dimension().location().toString());
		RenderUtils.options(graphics, this.font, minX, maxX, offsetY, padding, dimension, this.dimensionLeft, this.dimensionRight);

		offsetY += gap;
		RenderUtils.options(graphics, this.font, minX, maxX, offsetY, padding, clusters, this.clustersLeft, this.clustersRight);

		offsetY += gap;

		Component x = Component.literal("X");
		Component z = Component.literal("Z");
		this.chunkPosX.setWidth(centerX - minX - 5 * padding - this.font.width(x) + 2);
		this.chunkPosZ.setWidth(maxX - centerX - 5 * padding - this.font.width(z) + 2);
		RenderUtils.optionRight(graphics, this.font, minX, centerX + padding / 2, offsetY, padding, x, this.chunkPosX);
		RenderUtils.optionRight(graphics, this.font, centerX - padding / 2, maxX, offsetY, padding, z, this.chunkPosZ);

		offsetY += gap;
		this.returnToPlayer.setPosition(minX + padding, offsetY);
		this.returnToPlayer.setWidth(maxX - minX - 2 * padding);

		offsetY += gap + 3;
		RenderUtils.optionLeft(graphics, this.font, minX, maxX, offsetY, padding, stages, this.showStages);

		offsetY += gap;
		RenderUtils.optionLeft(graphics, this.font, minX, maxX, offsetY, padding, tickets, this.showTickets);

		offsetY += gap;
		RenderUtils.optionLeft(graphics, this.font, minX, maxX, offsetY, padding, minimap, this.showMinimap);

		offsetY += gap;
		RenderUtils.options(graphics, this.font, minX, maxX, offsetY, padding, corner, this.minimapCornerLeft, this.minimapCornerRight);

		offsetY += gap;
		RenderUtils.optionLeft(graphics, this.font, minX, maxX, offsetY, padding, fade, this.chunkRetention);

		graphics.pose().popPose();
	}

	private void renderChunkSelectionMenu(GuiGraphics graphics, DimensionState state) {
		this.breakdown.visible = state.selection != null;
		if (!this.breakdown.visible || !this.breakdown.isToggled()) {
			return;
		}

		graphics.pose().pushPose();

		ChunkSelectionInfo info = ChunkSelectionInfo.create(state.selection, state.chunks);

		int padding = MENU_PADDING;
		int width = info.getMaxWidth(this.font) + 4 * padding;
		int minX = this.width - width - padding;
		int minY = padding + 1 - 1;
		int maxX = this.width - padding;
		int maxY = this.height - padding;
		graphics.fill(minX, minY, maxX, maxY, HL_BG_LIGHT);

		graphics.drawString(this.font, info.title(), minX + padding, minY + padding, 0xFFFFFFFF);

		minX += padding;
		maxX -= padding;
		int offsetY = minY + padding * 2;
		for (List<Component> section : info.sections()) {
			offsetY = this.renderInnerChunkSelectionInfo(graphics, section, padding, minX, maxX, offsetY);
		}

		graphics.pose().popPose();
	}

	private int renderInnerChunkSelectionInfo(
		GuiGraphics graphics,
		List<Component> lines,
		int padding,
		int minX,
		int maxX,
		int offsetY
	) {
		if (lines.isEmpty()) {
			return offsetY;
		}

		int increment = this.font.lineHeight + padding;
		int maxY = offsetY + increment * (lines.size() + 1);
		graphics.fill(minX, offsetY + increment - padding, maxX, maxY, BG_DARK);
		for (Component line : lines) {
			offsetY += this.font.lineHeight + padding;
			graphics.drawString(this.font, line, minX + padding, offsetY, 0xFFFFFFFF);
		}
		return maxY - padding;
	}

	private void renderChunkSelection(GuiGraphics graphics, ChunkSelection selection, int color) {
		this.renderChunkSelection(graphics, selection, 0.3F, color);
	}

	private void renderChunkSelection(GuiGraphics graphics, ChunkSelection selection, float thickness, int color) {
		outline(graphics, selection.minX, selection.minZ, selection.sizeX(), selection.sizeZ(), thickness, color);
	}

	private void renderPlayer(GuiGraphics graphics, ChunkPos pos) {
		outline(graphics, pos.x, pos.z, 1, 1, 0.3F, PLAYER_COLOR);
	}

	private DimensionState state() {
		return this.state(this.dimension());
	}

	private DimensionState state(ResourceKey<Level> dimension) {
		return this.states.computeIfAbsent(dimension, dim -> {
			DimensionState state = new DimensionState(dim, this.executor);
			this.initializeState(state);
			return state;
		});
	}

	private void jumpToCluster(int offset) {
		DimensionState state = this.state();
		state.getCluster(this.clusterIndex + offset).thenApplyAsync(pair -> {
			ChunkSelection selection = pair.first();
			this.clusterIndex = pair.secondInt();
			this.setMapCenter(selection.getCenterChunkPos());
			this.clusterSelection = selection;
			this.clusterTicks = 20;
			return null;
		}, this.minecraft);
	}

	private void loadDimensions() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.getConnection() != null) {
			Set<ResourceKey<Level>> dimensions = minecraft.getConnection().levels();
			Set<ResourceKey<Level>> sorted = new LinkedHashSet<>();
			if (dimensions.contains(Level.OVERWORLD)) {
				sorted.add(Level.OVERWORLD);
			}
			if (dimensions.contains(Level.NETHER)) {
				sorted.add(Level.NETHER);
			}
			if (dimensions.contains(Level.END)) {
				sorted.add(Level.END);
			}
			sorted.addAll(dimensions);
			this.dimensions.addAll(sorted);
		} else {
			this.dimensions.add(Level.OVERWORLD);
			this.dimensions.add(Level.NETHER);
			this.dimensions.add(Level.END);
		}
		int width = this.dimensions.stream().mapToInt(key -> {
			return minecraft.font.width(key.location().toString());
		}).max().orElse(10);
		this.dimensionWidth = Math.min(width, 140);

		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) {
			this.dimensionIndex = this.dimensions.indexOf(player.level().dimension());
		}
	}

	private void initializeState(DimensionState state) {
		if (!this.isWatching(state.dimension)) {
			ChunkDebugClient.getInstance().startWatching(state.dimension);
		}
		if (!state.initialized) {
			ChunkPos center = ChunkPos.ZERO;
			if (this.minecraft != null) {
				LocalPlayer player = this.minecraft.player;
				if (player != null && player.level().dimension() == state.dimension) {
					center = player.chunkPosition();
				}
			}
			state.offsetX = (this.width / 2.0) - center.x * state.scale;
			state.offsetY = (this.height / 2.0) - center.z * state.scale;
			state.initialized = true;
		}
	}

	private void setMapCenter(ChunkPos pos) {
		this.setMapCenter(pos.x, pos.z);
	}

	private void setMapCenter(int x, int z) {
		DimensionState state = this.state();
		state.offsetX = (this.width / 2.0) - x * state.scale;
		state.offsetY = (this.height / 2.0) - z * state.scale;
	}

	private ChunkPos convertScreenToChunkPos(double x, double y) {
		DimensionState state = this.state();
		double scaledX = (x - state.offsetX) / state.scale;
		double scaledY = (y - state.offsetY) / state.scale;
		return new ChunkPos(Mth.floor(scaledX), Mth.floor(scaledY));
	}

	private void incrementDimension(int increment) {
		this.dimensionIndex = (this.dimensionIndex + increment + this.dimensions.size()) % this.dimensions.size();
	}

	private ResourceKey<Level> dimension() {
		if (this.dimensions.isEmpty()) {
			this.loadDimensions();
		}
		return this.dimensions.get(this.dimensionIndex);
	}

	private boolean isWatching(ResourceKey<Level> dimension) {
		return this.states.containsKey(dimension);
	}

	private int calculateChunkColor(ChunkData data) {
		ChunkPos pos = data.position();
		ChunkStatus stage = this.showStages.isToggled() ? data.stage() : null;
		List<Ticket<?>> tickets = this.showTickets.isToggled() ? data.tickets() : List.of();
		int color = ChunkColors.calculateChunkColor(data.status(), stage, tickets, data.unloading());
		if ((pos.x + pos.z) % 2 == 0) {
			color = ARGB.lerp(0.12F, color, 0xFFFFFF);
		}
		return color | 0xFF000000;
	}

	private enum Minimap {
		NONE, STATIC, FOLLOW;

		public Minimap previous() {
			return switch (this) {
				case NONE -> FOLLOW;
				case STATIC -> NONE;
				case FOLLOW -> STATIC;
			};
		}

		public Minimap next() {
			return switch (this) {
				case NONE -> STATIC;
				case STATIC -> FOLLOW;
				case FOLLOW -> NONE;
			};
		}

		public Component pretty() {
			return Component.translatable("chunk-debug.settings.minimap." + this.name().toLowerCase());
		}
	}

	private static class DimensionState {

		private final Multimap<Integer, ChunkData> unloaded = HashMultimap.create();

		private final Long2ObjectMap<ChunkData> chunks = new Long2ObjectOpenHashMap<>();
		private final ChunkClusters clusters = new ChunkClusters();
		private final ResourceKey<Level> dimension;

		private final Executor clusterWorker;

		private ChunkSelection selection;
		private ChunkPos first;

		private float scale = 1.0F;

		private double offsetX = 0.0;
		private double offsetY = 0.0;

		boolean initialized = false;

		private DimensionState(ResourceKey<Level> dimension, Executor clusterWorker) {
			this.dimension = dimension;
			this.clusterWorker = clusterWorker;
		}

		void add(long pos, ChunkData data) {
			this.chunks.put(pos, data);
			this.clusterWorker.execute(() -> {
				this.clusters.add(pos);
			});
		}

		void remove(long pos, int tick) {
			this.clusterWorker.execute(() -> {
				this.clusters.remove(pos);
			});
			ChunkData data = this.chunks.remove(pos);
			if (data != null) {
				this.unloaded.put(tick, data);
				data.updateUnloading(false);
			}
		}

		CompletableFuture<ObjectIntPair<ChunkSelection>> getCluster(int index) {
			return CompletableFuture.supplyAsync(() -> {
				int corrected = (index + this.clusters.count()) % this.clusters.count();
				LongSet cluster = this.clusters.getCluster(corrected);
				List<ChunkPos> positions = cluster.longStream().mapToObj(ChunkPos::new).toList();
				return ObjectIntPair.of(ChunkSelection.fromPositions(positions), corrected);
			}, this.clusterWorker);
		}
	}
}
