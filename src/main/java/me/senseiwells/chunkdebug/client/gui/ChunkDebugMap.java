package me.senseiwells.chunkdebug.client.gui;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import me.senseiwells.chunkdebug.client.ChunkDebugClient;
import me.senseiwells.chunkdebug.client.config.ChunkDebugClientConfig;
import me.senseiwells.chunkdebug.client.gui.state.ColoredChunkDataRenderState;
import me.senseiwells.chunkdebug.client.utils.Bounds;
import me.senseiwells.chunkdebug.common.utils.ImmutableChunkData;
import me.senseiwells.chunkdebug.common.utils.ImmutableChunkData.Ticket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static me.senseiwells.chunkdebug.client.utils.RenderUtils.*;
import static me.senseiwells.chunkdebug.client.utils.RenderUtils.HL_BG_DARK;

public class ChunkDebugMap {
	private static final int SELECTING_OUTLINE_COLOR = 0xAAAA0000;
	private static final int SELECTED_OUTLINE_COLOR = 0xAAFF0000;
	private static final int CLUSTER_OUTLINE_COLOR = HL;
	private static final int PLAYER_COLOR = 0xAAFFFF00;

	private static final float MINIMAP_SCALE = 0.5F;

	private final Minecraft minecraft;
	private final ChunkDebugClient client;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final Map<ResourceKey<Level>, DimensionState> states = new Object2ObjectOpenHashMap<>();
	private final List<ResourceKey<Level>> dimensions = new ArrayList<>();

	@Nullable private ChunkSelection clusterSelection;
	private int clusterTicks = 0;
	private int clusterIndex = 0;

	private int width;
	private int height;

	private int ticks = 0;

	final ChunkDebugClientConfig config;

	ChunkPos center = ChunkPos.ZERO;

	int dimensionWidth = 0;
	int dimensionIndex = 0;

	public ChunkDebugMap(Minecraft minecraft, ChunkDebugClient client) {
		this.minecraft = minecraft;
		this.client = client;
		this.config = client.config;

		this.width = this.minecraft.getWindow().getGuiScaledWidth();
		this.height = this.minecraft.getWindow().getGuiScaledHeight();
	}

	public void updateChunks(ResourceKey<Level> dimension, Collection<ImmutableChunkData> chunks) {
		DimensionState state = this.state(dimension);
		for (ImmutableChunkData chunk : chunks) {
			state.add(chunk.position().pack(), chunk);
		}
	}

	public void unloadChunks(ResourceKey<Level> dimension, long[] positions) {
		DimensionState state = this.state(dimension);
		for (long position : positions) {
			state.remove(position, this.ticks + this.config.chunkRetention);
		}
	}

	public void tick() {
		if (this.clusterTicks > 0) {
			this.clusterTicks--;
		} else {
			this.clusterSelection = null;
		}

		int tick = this.ticks++;
		for (DimensionState state : this.states.values()) {
			state.unloaded.removeAll(tick);
		}
	}

	public void resize(int width, int height) {
		DimensionState state = this.state();

		float oldCenterX = this.width / 2.0F;
		float oldCenterY = this.height / 2.0F;

		float currentX = (oldCenterX - state.offsetX) / state.scale;
		float currentY = (oldCenterY - state.offsetY) / state.scale;

		this.width = width;
		this.height = height;

		float newCenterX = width / 2.0F;
		float newCenterY = height / 2.0F;
		state.offsetX = newCenterX - currentX * state.scale;
		state.offsetY = newCenterY - currentY * state.scale;

		this.updateCenter();
	}

	public void close() {
		this.executor.shutdown();
	}

	public void renderMinimap(GuiGraphicsExtractor graphics) {
		if (this.config.minimapMode == Minimap.NONE || this.minecraft.player == null) {
			return;
		}

		Bounds bounds = this.getMinimapBounds();
		int minX = bounds.minX(), minY = bounds.minY(), maxX = bounds.maxX(), maxY = bounds.maxY();

		graphics.pose().pushMatrix();

		graphics.fill(minX - 3, minY - 3, maxX + 3, maxY + 3, HL_BG_LIGHT);
		graphics.fill(minX, minY, maxX, maxY, HL_BG_DARK);

		graphics.enableScissor(minX, minY, maxX, maxY);
		graphics.pose().translate(minX + this.config.minimapSize / 2.0F, minY + this.config.minimapSize / 2.0F);

		DimensionState state;
		if (this.config.minimapMode == Minimap.STATIC) {
			state = this.state();
			float offsetX = (this.width / 2.0F - state.offsetX) / state.scale;
			float offsetY = (this.height / 2.0F - state.offsetY) / state.scale;

			graphics.pose().scale(state.scale * MINIMAP_SCALE, state.scale * MINIMAP_SCALE);
			graphics.pose().translate(-offsetX, -offsetY);
		} else {
			LocalPlayer player = this.minecraft.player;
			ResourceKey<Level> dimension = player.level().dimension();
			state = this.state(dimension);
			ChunkPos pos = player.chunkPosition();

			graphics.pose().scale(state.scale * MINIMAP_SCALE, state.scale * MINIMAP_SCALE);
			graphics.pose().translate(-pos.x() - 0.5F, -pos.z() - 0.5F);
		}

		this.renderMap(graphics, state);

		graphics.disableScissor();
		graphics.pose().popMatrix();
	}

	void renderMap(GuiGraphicsExtractor graphics, DimensionState state) {
		Int2ObjectMap<List<ChunkPos>> states = new Int2ObjectOpenHashMap<>();
		for (ImmutableChunkData data : state.chunks.values()) {
			ChunkPos pos = data.position();
			int color = this.calculateChunkColor(data);
			List<ChunkPos> positions = states.computeIfAbsent(color, c -> new ObjectArrayList<>());
			positions.add(pos);
		}

		if (this.config.chunkRetention > 0) {
			for (Map.Entry<Integer, ImmutableChunkData> entry : state.unloaded.entries()) {
				float delta = (float) (entry.getKey() - this.ticks) / this.config.chunkRetention;
				int alpha = ((byte) (delta * 255)) << 24 | 0xFFFFFF;
				ImmutableChunkData data = entry.getValue();
				ChunkPos pos = data.position();
				int color = this.calculateChunkColor(data) & alpha;
				List<ChunkPos> positions = states.computeIfAbsent(color, c -> new ObjectArrayList<>());
				positions.add(pos);
			}
		}

		Matrix3x2f matrix = new Matrix3x2f(graphics.pose());
		ScreenRectangle scissor = graphics.scissorStack.peek();
		ColoredChunkDataRenderState data = new ColoredChunkDataRenderState(
			RenderPipelines.GUI, TextureSetup.noTexture(), matrix, states, scissor
		);
		graphics.guiRenderState.addGuiElement(data);

		if (state.selection != null) {
			this.renderChunkSelection(graphics, state.selection, SELECTED_OUTLINE_COLOR);
		}

		if (this.minecraft.player != null) {
			if (this.minecraft.player.level().dimension() == state.dimension) {
				this.renderPlayer(graphics, this.minecraft.player.chunkPosition());
			}
		}
	}

	void renderChunkSelecting(GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
		DimensionState state = this.state();
		if (state.first != null) {
			ChunkSelection selection = new ChunkSelection(state.first, this.convertScreenToChunkPos(mouseX, mouseY));
			this.renderChunkSelection(graphics, selection, SELECTING_OUTLINE_COLOR);
		}
	}

	void renderChunkClusters(GuiGraphicsExtractor graphics) {
		if (this.clusterSelection != null) {
			this.renderChunkSelection(graphics, this.clusterSelection, 2.0F, CLUSTER_OUTLINE_COLOR);
		}
	}

	void incrementDimension(int increment) {
		this.dimensionIndex = (this.dimensionIndex + increment + this.dimensions.size()) % this.dimensions.size();
	}

	void nextMinimap() {
		this.config.minimapMode = this.config.minimapMode.next();
	}

	void previousMinimap() {
		this.config.minimapMode = this.config.minimapMode.previous();
	}

	void returnToPlayer() {
		if (this.minecraft.player != null) {
			this.dimensionIndex = this.dimensions.indexOf(this.minecraft.player.level().dimension());
			if (this.dimensionIndex != -1) {
				this.setMapCenter(this.minecraft.player.chunkPosition());
			} else {
				this.dimensionIndex = 0;
			}
		}
	}

	void jumpToCluster(int offset) {
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

	void setMapCenterX(int x) {
		this.setMapCenter(x, this.center.z());
	}

	void setMapCenterZ(int z) {
		this.setMapCenter(this.center.x(), z);
	}

	void updateCenter() {
		this.center = this.convertScreenToChunkPos(this.width / 2.0, this.height / 2.0);
	}

	DimensionState state() {
		return this.state(this.dimension());
	}

	DimensionState state(ResourceKey<Level> dimension) {
		return this.states.computeIfAbsent(dimension, dim -> {
			DimensionState state = new DimensionState(dim, this.executor);
			this.initializeState(state);
			return state;
		});
	}

	void resetData() {
		for (DimensionState state : this.states.values()) {
			state.chunks.clear();
			state.unloaded.clear();
			state.clusters.clear();
		}
	}

	void resetStates() {
		this.states.clear();
	}

	Component getMinimapName() {
		return this.config.minimapMode.pretty();
	}

	Bounds getMinimapBounds() {
		int padding = 10;

		int minX = this.config.minimapCorner.isLeft() ? padding : this.width - this.config.minimapSize - padding;
		int minY = this.config.minimapCorner.isTop() ? padding : this.height - this.config.minimapSize - padding;
		int maxX = minX + this.config.minimapSize;
		int maxY = minY + this.config.minimapSize;
		return new Bounds(minX, minY, maxX, maxY).offset((int) this.config.minimapOffsetX, (int) this.config.minimapOffsetY);
	}

	ChunkPos convertScreenToChunkPos(double x, double y) {
		DimensionState state = this.state();
		double scaledX = (x - state.offsetX) / state.scale;
		double scaledY = (y - state.offsetY) / state.scale;
		return new ChunkPos(Mth.floor(scaledX), Mth.floor(scaledY));
	}

	ResourceKey<Level> dimension() {
		if (this.dimensions.isEmpty()) {
			this.loadDimensions();
		}
		return this.dimensions.get(this.dimensionIndex);
	}

	private void renderChunkSelection(GuiGraphicsExtractor graphics, ChunkSelection selection, int color) {
		this.renderChunkSelection(graphics, selection, 0.4F, color);
	}

	private void renderChunkSelection(GuiGraphicsExtractor graphics, ChunkSelection selection, float thickness, int color) {
		outline(graphics, selection.minX, selection.minZ, selection.sizeX(), selection.sizeZ(), thickness, color);
	}

	private void renderPlayer(GuiGraphicsExtractor graphics, ChunkPos pos) {
		outline(graphics, pos.x(), pos.z(), 1, 1, 0.3F, PLAYER_COLOR);
	}

	private boolean isWatching(ResourceKey<Level> dimension) {
		return this.states.containsKey(dimension);
	}

	private void setMapCenter(ChunkPos pos) {
		this.setMapCenter(pos.x(), pos.z());
	}

	private void setMapCenter(int x, int z) {
		DimensionState state = this.state();
		state.offsetX = (this.width / 2.0F) - x * state.scale;
		state.offsetY = (this.height / 2.0F) - z * state.scale;
		this.center = new ChunkPos(x, z);
	}

	private void initializeState(DimensionState state) {
		if (!this.isWatching(state.dimension)) {
			ChunkDebugClient.getInstance().startWatching(state.dimension);
		}
		if (!state.initialized) {
			ChunkPos center = ChunkPos.ZERO;
			LocalPlayer player = this.minecraft.player;
			if (player != null && player.level().dimension() == state.dimension) {
				center = player.chunkPosition();
			}
			state.offsetX = (this.width / 2.0F) - center.x() * state.scale;
			state.offsetY = (this.height / 2.0F) - center.z() * state.scale;
			state.initialized = true;
		}
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
			return minecraft.font.width(key.identifier().toString());
		}).max().orElse(10);
		this.dimensionWidth = Math.min(width, 140);

		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) {
			this.dimensionIndex = this.dimensions.indexOf(player.level().dimension());
		}
	}

	private int calculateChunkColor(ImmutableChunkData data) {
		ChunkPos pos = data.position();
		ChunkStatus stage = this.client.config.showStages ? data.stage() : null;
		List<Ticket> tickets = this.client.config.showTickets ? data.tickets() : List.of();
		int color = ChunkColors.calculateChunkColor(data.status(), stage, tickets, data.unloading());
		if ((pos.x() + pos.z()) % 2 == 0) {
			color = ARGB.srgbLerp(0.12F, color, 0xFFFFFF);
		}
		return color | 0xFF000000;
	}

	public enum Minimap implements StringRepresentable {
		NONE, STATIC, FOLLOW;

        public static final Codec<Minimap> CODEC = StringRepresentable.fromEnum(Minimap::values);

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

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase();
        }
    }

	static class DimensionState {
		private final Multimap<Integer, ImmutableChunkData> unloaded = HashMultimap.create();

		private final Long2ObjectMap<ImmutableChunkData> chunks = new Long2ObjectOpenHashMap<>();
		private final ChunkClusters clusters = new ChunkClusters();
		private final ResourceKey<Level> dimension;

		private final Executor clusterWorker;

		private boolean initialized = false;

		@Nullable ChunkSelection selection;
		@Nullable ChunkPos first;

		float scale = 1.0F;

		float offsetX = 0.0F;
		float offsetY = 0.0F;

		private DimensionState(ResourceKey<Level> dimension, Executor clusterWorker) {
			this.dimension = dimension;
			this.clusterWorker = clusterWorker;
		}

		Long2ObjectMap<ImmutableChunkData> chunks() {
			return this.chunks;
		}

		private void add(long pos, ImmutableChunkData data) {
			this.chunks.put(pos, data);
			this.clusterWorker.execute(() -> {
				this.clusters.add(pos);
			});
		}

		private void remove(long pos, int tick) {
			this.clusterWorker.execute(() -> {
				this.clusters.remove(pos);
			});
			if (this.chunks.containsKey(pos)) {
				ImmutableChunkData data = this.chunks.remove(pos);
				this.unloaded.put(tick, data.withoutUnloading());
			}
		}

		private CompletableFuture<ObjectIntPair<ChunkSelection>> getCluster(int index) {
			return CompletableFuture.supplyAsync(() -> {
				int corrected = (index + this.clusters.count()) % this.clusters.count();
				LongSet cluster = this.clusters.getCluster(corrected);
				List<ChunkPos> positions = cluster.longStream().mapToObj(ChunkPos::unpack).toList();
				return ObjectIntPair.of(ChunkSelection.fromPositions(positions), corrected);
			}, this.clusterWorker);
		}
	}
}
