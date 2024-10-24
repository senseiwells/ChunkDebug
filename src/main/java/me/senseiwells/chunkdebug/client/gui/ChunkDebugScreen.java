package me.senseiwells.chunkdebug.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import me.senseiwells.chunkdebug.client.ChunkDebugClient;
import me.senseiwells.chunkdebug.client.gui.widget.ArrowButton;
import me.senseiwells.chunkdebug.client.gui.widget.ArrowButton.Direction;
import me.senseiwells.chunkdebug.client.gui.widget.NamedButton;
import me.senseiwells.chunkdebug.client.gui.widget.IntegerEditbox;
import me.senseiwells.chunkdebug.client.gui.widget.ToggleButton;
import me.senseiwells.chunkdebug.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.senseiwells.chunkdebug.client.utils.RenderUtils.*;

public class ChunkDebugScreen extends Screen {
	private static final int MENU_PADDING = 3;

	private final ChunkDebugMap map;
	private final Screen parent;

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

	public ChunkDebugScreen(ChunkDebugMap map, @Nullable Screen parent) {
		super(Component.translatable("chunk-debug.screen.title"));
		this.map = map;
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();

		this.breakdown = new ToggleButton(this.width - 20, this.height - 20, 15);
		this.breakdown.setTooltip(Tooltip.create(Component.translatable("chunk-debug.info.breakdown.toggle")));
		this.breakdown.setToggled(true);
		this.addRenderableWidget(this.breakdown);

		this.settings = new ToggleButton(5, this.height - 20, 15);
		this.settings.setTooltip(Tooltip.create(Component.translatable("chunk-debug.settings.toggle")));
		this.settings.setToggled(true);
		this.addRenderableWidget(this.settings);

		this.dimensionLeft = new ArrowButton(Direction.LEFT, 9, 0, 15, () -> this.map.incrementDimension(-1));
		this.addRenderableWidget(this.dimensionLeft);
		this.dimensionRight = new ArrowButton(Direction.RIGHT, 0, 0, 15, () -> this.map.incrementDimension(1));
		this.addRenderableWidget(this.dimensionRight);

		this.minimapLeft = new ArrowButton(Direction.LEFT, 0, 0, 15, this.map::previousMinimap);
		this.addRenderableWidget(this.minimapLeft);
		this.minimapRight = new ArrowButton(Direction.RIGHT, 0, 0, 15, this.map::nextMinimap);
		this.addRenderableWidget(this.minimapRight);

		this.minimapCornerLeft = new ArrowButton(Direction.LEFT, 0, 0, 15, () -> {
			this.map.config.minimapCorner = this.map.config.minimapCorner.previous();
			this.map.config.minimapOffsetX = this.map.config.minimapOffsetY = 0;
		});
		this.addRenderableWidget(this.minimapCornerLeft);
		this.minimapCornerRight = new ArrowButton(Direction.RIGHT, 0, 0, 15, () -> {
			this.map.config.minimapCorner = this.map.config.minimapCorner.next();
			this.map.config.minimapOffsetX = this.map.config.minimapOffsetY = 0;
		});
		this.addRenderableWidget(this.minimapCornerRight);

		Component player = Component.translatable("chunk-debug.settings.return");
		this.returnToPlayer = new NamedButton(0, 0, 0, 15, player, this.map::returnToPlayer);
		this.addRenderableWidget(this.returnToPlayer);

		this.clustersLeft = new ArrowButton(Direction.LEFT, 0, 0, 15, () -> this.map.jumpToCluster(-1));
		this.addRenderableWidget(this.clustersLeft);
		this.clustersRight = new ArrowButton(Direction.RIGHT, 0, 0, 15, () -> this.map.jumpToCluster(1));
		this.addRenderableWidget(this.clustersRight);

		this.chunkPosX = new IntegerEditbox(this.font, 40, 15, this.map::setMapCenterX);
		this.addRenderableWidget(this.chunkPosX);
		this.chunkPosZ = new IntegerEditbox(this.font, 40, 15, this.map::setMapCenterZ);
		this.addRenderableWidget(this.chunkPosZ);

		this.showStages = new ToggleButton(0, 0, 15, b -> this.map.config.showStages = b);
		this.showStages.setToggled(this.map.config.showStages);
		this.addRenderableWidget(this.showStages);

		this.showTickets = new ToggleButton(0, 0, 15, b -> this.map.config.showTickets = b);
		this.showTickets.setToggled(this.map.config.showTickets);
		this.addRenderableWidget(this.showTickets);

		this.showMinimap = new ToggleButton(0, 0, 15, b -> this.map.config.showMinimap = b);
		this.showMinimap.setTooltip(Tooltip.create(Component.translatable("chunk-debug.settings.visibility.minimap.tooltip")));
		this.showMinimap.setToggled(this.map.config.showMinimap);
		this.addRenderableWidget(this.showMinimap);

		this.chunkRetention = new IntegerEditbox(this.font, 30, 15, i -> this.map.config.chunkRetention = i);
		this.chunkRetention.setTooltip(Tooltip.create(Component.translatable("chunk-debug.settings.visibility.unload.tooltip")));
		this.chunkRetention.setIntValue(this.map.config.chunkRetention);
		this.addRenderableWidget(this.chunkRetention);

		// Tick jump [<] [7832] [>]
	}

	@Override
	protected void repositionElements() {
		this.breakdown.setPosition(this.width - 20, this.height - 20);
		this.settings.setY(this.height - 20);
	}

	@Override
	public void added() {
		ChunkDebugClient.getInstance().startWatching(this.map.dimension());
	}

	@Override
	public void removed() {
		if (this.map.minimap == ChunkDebugMap.Minimap.NONE) {
			ChunkDebugClient.getInstance().stopWatching();
			this.map.resetStates();
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
		this.renderBlurredBackground();

		ChunkDebugMap.DimensionState state = this.map.state();

		graphics.pose().pushPose();
		graphics.pose().translate(state.offsetX, state.offsetY, 0.0);
		graphics.pose().scale(state.scale, state.scale, 0.0F);

		this.map.renderMap(graphics, state);

		this.map.renderChunkSelecting(graphics, mouseX, mouseY);
		this.map.renderChunkClusters(graphics);

		graphics.pose().popPose();

		if (this.showMinimap.isToggled()) {
			this.map.renderMinimap(graphics);
		}

		this.renderChunkSelectionMenu(graphics, state);
		this.renderSettingsMenu(graphics);

		if (!this.chunkPosX.isFocused()) {
			this.chunkPosX.setIntValue(this.map.center.x);
		}
		if (!this.chunkPosZ.isFocused()) {
			this.chunkPosZ.setIntValue(this.map.center.z);
		}

		super.render(graphics, mouseX, mouseY, partial);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		if (keyCode == InputConstants.KEY_F1) {
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
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
			return;
		}
		super.onClose();
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
		if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
			ChunkDebugMap.DimensionState state = this.map.state();
			state.first = this.map.convertScreenToChunkPos(mouseX, mouseY);
			return true;
		}
		if (button == InputConstants.MOUSE_BUTTON_LEFT) {
			if (this.showMinimap.isToggled() && this.map.getMinimapBounds().contains(mouseX, mouseY)) {
				this.draggingMinimap = true;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		ChunkDebugMap.DimensionState state = this.map.state();
		if (state.first != null && button == InputConstants.MOUSE_BUTTON_RIGHT) {
			ChunkSelection selection = new ChunkSelection(state.first, this.map.convertScreenToChunkPos(mouseX, mouseY));
			state.first = null;
			if (selection.equals(state.selection)) {
				state.selection = null;
			} else {
				state.selection = selection;
			}
			return true;
		}
		if (button == InputConstants.MOUSE_BUTTON_LEFT) {
			this.draggingMinimap = false;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		ChunkDebugMap.DimensionState state = this.map.state();
		if (button == InputConstants.MOUSE_BUTTON_LEFT) {
			if (this.draggingMinimap) {
				this.map.config.minimapOffsetX += dragX;
				this.map.config.minimapOffsetY += dragY;
			} else {
				state.offsetX += dragX;
				state.offsetY += dragY;
				this.map.updateCenter();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.map.getMinimapBounds().contains(mouseX, mouseY)) {
			this.map.config.minimapSize = Mth.clamp(this.map.config.minimapSize + (int) scrollY, 20, 200);
			return true;
		}

		ChunkDebugMap.DimensionState state = this.map.state();
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
		int width = Math.max(this.map.dimensionWidth, RenderUtils.maxWidth(this.font, clusters, minimap)) + 2 * padding + 30;
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

		Component mode = this.map.getMinimapName();
		RenderUtils.options(graphics, this.font, minX, maxX, offsetY, padding, mode, this.minimapLeft, this.minimapRight);

		offsetY += gap + 3;
		Component dimension = Component.literal(this.map.dimension().location().toString());
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

	private void renderChunkSelectionMenu(GuiGraphics graphics, ChunkDebugMap.DimensionState state) {
		this.breakdown.visible = state.selection != null;
		if (!this.breakdown.visible || !this.breakdown.isToggled()) {
			return;
		}

		graphics.pose().pushPose();

		ChunkSelectionInfo info = ChunkSelectionInfo.create(state.selection, state.chunks());

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
}
