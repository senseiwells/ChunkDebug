package me.senseiwells.chunkdebug.client.gui;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.senseiwells.chunkdebug.client.ChunkDebugClient;
import me.senseiwells.chunkdebug.client.gui.widget.ArrowButton;
import me.senseiwells.chunkdebug.client.gui.widget.ArrowButton.Direction;
import me.senseiwells.chunkdebug.client.gui.widget.NamedButton;
import me.senseiwells.chunkdebug.client.gui.widget.ToggleButton;
import me.senseiwells.chunkdebug.client.utils.RenderUtils;
import me.senseiwells.chunkdebug.common.utils.ChunkData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static me.senseiwells.chunkdebug.client.utils.RenderUtils.*;

public class ChunkDebugScreen extends Screen {
	private static final int SELECTING_OUTLINE_COLOR = 0xAAAA0000;
	private static final int SELECTED_OUTLINE_COLOR = 0xAAFF0000;
	private static final int PLAYER_COLOR = 0xAAFFFF00;

	private static final float MINIMAP_SCALE = 0.5F;

	private static final int MENU_PADDING = 3;

	private final Map<ResourceKey<Level>, DimensionState> states = new Object2ObjectOpenHashMap<>();
	private final List<ResourceKey<Level>> dimensions = new ArrayList<>();
	private int dimensionWidth = 0;
	private int dimensionIndex = 0;

	private Minimap minimap = Minimap.FOLLOW;

	private ToggleButton breakdown;
	private ToggleButton settings;

	private ArrowButton dimensionLeft;
	private ArrowButton dimensionRight;

	private ArrowButton minimapLeft;
	private ArrowButton minimapRight;

	private NamedButton returnToPlayer;

	private ToggleButton minimapOnTop;
	private ToggleButton showStages;
	private ToggleButton showTickets;

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

	public void unloadChunks(ResourceKey<Level> dimension, long[] positions) {
		DimensionState state = this.state(dimension);
		for (long position : positions) {
			state.chunks.remove(position);
		}
	}

	@Override
	protected void init() {
		super.init();
		this.initialized = true;

		this.initializeDimension(this.dimension());

		this.breakdown = new ToggleButton(this.width - 20, this.height - 20, 15);
		this.breakdown.setTooltip(Tooltip.create(Component.translatable("chunk-debug.info.breakdown.toggle")));
		this.breakdown.setToggled(true);
		this.addRenderableWidget(this.breakdown);

		this.settings = new ToggleButton(5, this.height - 20, 15);
		this.settings.setTooltip(Tooltip.create(Component.translatable("chunk-debug.settings.toggle")));
		this.settings.setToggled(true);
		this.addRenderableWidget(this.settings);

		this.dimensionLeft = new ArrowButton(Direction.LEFT, 5, 30, 15, () -> {
			this.dimensionIndex = (this.dimensionIndex - 1 + this.dimensions.size()) % this.dimensions.size();
			this.initializeDimension(this.dimension());
		});
		this.addRenderableWidget(this.dimensionLeft);
		this.dimensionRight = new ArrowButton(Direction.RIGHT, 5, 30, 15, () -> {
			this.dimensionIndex = (this.dimensionIndex + 1 + this.dimensions.size()) % this.dimensions.size();
			this.initializeDimension(this.dimension());
		});
		this.addRenderableWidget(this.dimensionRight);

		this.minimapLeft = new ArrowButton(Direction.LEFT, 0, 0, 15, () -> this.minimap = this.minimap.previous());
		this.addRenderableWidget(this.minimapLeft);
		this.minimapRight = new ArrowButton(Direction.RIGHT, 0, 0, 15, () -> this.minimap = this.minimap.next());
		this.addRenderableWidget(this.minimapRight);

		Component player = Component.translatable("chunk-debug.settings.return");
		this.returnToPlayer = new NamedButton(0, 0, 0, 15, player, () -> {
			if (this.minecraft != null && this.minecraft.player != null) {
				this.dimensionIndex = this.dimensions.indexOf(this.minecraft.player.level().dimension());
				this.setMapCenter(this.minecraft.player.chunkPosition());
			}
		});
		this.addRenderableWidget(this.returnToPlayer);

		// Cluster button [<] clusters [>]

		// Chunk positions X:[ ] Z:[ ]

		// Tick jump [<] [7832] [>]
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

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
		this.renderBlurredBackground(partial);

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

		this.renderChunkSelectionMenu(graphics, state);
		this.renderSettingsMenu(graphics);

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
		int size = 100;
		int padding = 10;

		int minX = this.width - size - padding;
		int minY = padding + 1 - 1;
		int maxX = this.width - padding;
		int maxY = size + padding;

		graphics.pose().pushPose();

		graphics.fill(minX - 3, minY - 3, maxX + 3, maxY + 3, HL_BG_LIGHT);
		graphics.fill(minX, minY, maxX, maxY, HL_BG_DARK);

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
			ResourceKey<Level> dimension = player.level().dimension();
			this.initializeDimension(dimension);
			state = this.state(dimension);
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
			graphics.fill(pos.x, pos.z, pos.x + 1, pos.z + 1, this.calculateChunkColor(data));
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
		this.dimensionLeft.visible = this.dimensionRight.visible = this.settings.isToggled();
		this.minimapLeft.visible = this.minimapRight.visible = this.settings.isToggled();
		this.returnToPlayer.visible = this.settings.isToggled();
		if (!this.settings.isToggled()) {
			return;
		}

		graphics.pose().pushPose();
		Component title = Component.translatable("chunk-debug.settings").withColor(HL);
		Component player = Component.translatable("chunk-debug.settings.return");
		Component clusters = Component.translatable("chunk-debug.settings.clusters");

		int padding = MENU_PADDING;
		int width = Math.max(this.font.width(title), this.font.width(player));
		width = Math.max(width, this.font.width(clusters) + 4 * padding + 30);
		width = Math.max(width, this.dimensionWidth + 2 * padding + 30);
		width += 4 * padding;
		int minX = padding + 1 - 1;
		int minY = padding + 1 - 1;
		int maxX = padding + width;
		int maxY = this.height - padding;

		graphics.fill(minX, minY, maxX, maxY, HL_BG_LIGHT);
		graphics.drawString(this.font, title, minX + padding, minY + padding, 0xFFFFFFFF);

		int offsetY = minY + padding * 2 + this.font.lineHeight;
		Component dimension = Component.literal(this.dimension().location().toString());
		RenderUtils.options(graphics, this.font, minX, maxX, offsetY, padding, dimension, this.dimensionLeft, this.dimensionRight);

		offsetY += 2 * padding + 15;
		this.returnToPlayer.setPosition(minX + padding, offsetY);
		this.returnToPlayer.setWidth(maxX - minX - 2 * padding);

		offsetY += 2 * padding + 15;
		Component minimap = this.minimap.pretty();
		RenderUtils.options(graphics, this.font, minX, maxX, offsetY, padding, minimap, this.minimapLeft, this.minimapRight);

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
		outline(graphics, selection.minX, selection.minZ, selection.sizeX(), selection.sizeZ(), 0.3F, color);
	}

	private void renderPlayer(GuiGraphics graphics, ChunkPos pos) {
		outline(graphics, pos.x, pos.z, 1, 1, 0.3F, PLAYER_COLOR);
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

	private void initializeDimension(ResourceKey<Level> dimension) {
		if (!this.isWatching(dimension)) {
			ChunkDebugClient.getInstance().startWatching(dimension);
		}
		DimensionState state = this.state(dimension);
		if (!state.initialized) {
			ChunkPos center = ChunkPos.ZERO;
			if (this.minecraft != null) {
				LocalPlayer player = this.minecraft.player;
				if (player != null && player.level().dimension() == dimension) {
					center = player.chunkPosition();
				}
			}
			state.offsetX = this.width / 2.0 - center.x * state.scale;
			state.offsetY = this.height / 2.0 - center.z * state.scale;
			state.initialized = true;
		}
	}

	private void setMapCenter(ChunkPos pos) {
		DimensionState state = this.state();
		state.offsetX = (this.width / 2.0) - pos.x * state.scale;
		state.offsetY = (this.height / 2.0) - pos.z * state.scale;
	}

	private ChunkPos convertScreenToChunkPos(double x, double y) {
		DimensionState state = this.state();
		double scaledX = (x - state.offsetX) / state.scale;
		double scaledY = (y - state.offsetY) / state.scale;
		return new ChunkPos(Mth.floor(scaledX), Mth.floor(scaledY));
	}

	private int calculateChunkColor(ChunkData data) {
		ChunkPos pos = data.position();
		int color = ChunkColors.calculateChunkColor(data.status(), data.stage(), data.tickets(), data.unloading());
		if ((pos.x + pos.z) % 2 == 0) {
			color = FastColor.ARGB32.lerp(0.12F, color, 0xFFFFFF);
		}
		return color | 0xFF000000;
	}

	private DimensionState state() {
		return this.state(this.dimension());
	}

	private DimensionState state(ResourceKey<Level> dimension) {
		return this.states.computeIfAbsent(dimension, DimensionState::new);
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

	private static void bound(int minX, int minY, int maxX, int maxY, BoundsConsumer carrier) {
		carrier.apply(minX, minY, maxX, maxY);
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
		private final Long2ObjectMap<ChunkData> chunks = new Long2ObjectOpenHashMap<>();
		private final ResourceKey<Level> dimension;

		private ChunkSelection selection;
		private ChunkPos first;

		private float scale = 1.0F;

		private double offsetX = 0.0;
		private double offsetY = 0.0;

		boolean initialized = false;

		private DimensionState(ResourceKey<Level> dimension) {
			this.dimension = dimension;
		}
	}

	private interface BoundsConsumer {
		 void apply(int minX, int minY, int maxX, int maxY);
	}
}
