package me.senseiwells.chunkdebug.server.tracker;

import it.unimi.dsi.fastutil.longs.*;
import me.senseiwells.chunkdebug.ChunkDebug;
import me.senseiwells.chunkdebug.common.utils.ChunkData;
import net.minecraft.server.level.*;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChunkDebugTracker {
	private final Long2ObjectMap<ChunkData> chunks = new Long2ObjectOpenHashMap<>();
	private final LongSet dirty = new LongOpenHashSet();

	private final Long2ObjectMap<ChunkStatus> stages = new Long2ObjectOpenHashMap<>();

	private final ServerLevel level;

	public ChunkDebugTracker(ServerLevel level) {
		this.level = level;
	}

	public Collection<ChunkData> getChunks() {
		return this.chunks.values();
	}

	public DirtyChunks getDirtyChunks() {
		List<ChunkData> updated = new ArrayList<>(this.dirty.size());
		LongList removed = new LongArrayList();
		LongIterator iter = this.dirty.iterator();
		while (iter.hasNext()) {
			long pos = iter.nextLong();
			ChunkData data = this.chunks.get(pos);
			if (data != null) {
				updated.add(data);
			} else {
				removed.add(pos);
			}
		}
		this.dirty.clear();
		return new DirtyChunks(updated, removed);
	}

	public void tick() {
		synchronized (this.stages) {
			for (Long2ObjectMap.Entry<ChunkStatus> entry : this.stages.long2ObjectEntrySet()) {
				long pos = entry.getLongKey();
				ChunkData data = this.chunks.get(pos);
				if (data != null) {
					data.updateStage(entry.getValue());
					this.markDirty(pos);
				}
			}

			this.stages.clear();
		}
	}

	public void set(ChunkData data) {
		long pos = data.position().toLong();
		this.chunks.put(pos, data);
		this.markDirty(pos);
	}

	public void unload(long pos) {
		this.chunks.remove(pos);
		this.markDirty(pos);
	}

	public void updateStage(long pos, ChunkStatus stage) {
		synchronized (this.stages) {
			this.stages.put(pos, stage);
		}
	}

	public void updateTickets(long pos, SortedArraySet<Ticket<?>> tickets) {
		ChunkData data = this.chunks.get(pos);
		if (data != null) {
			data.updateTickets(tickets);
			this.markDirty(pos);
		}
	}

	public void updateTickingStatusLevel(long pos, int level) {
		ChunkData data = this.chunks.get(pos);
		if (data != null) {
			data.updateTickingStatusLevel(level);
			this.markDirty(pos);
		}
	}

	public void updateUnloading(long pos, boolean unloading) {
		ChunkData data = this.chunks.get(pos);
		if (data != null) {
			data.updateUnloading(unloading);
			this.markDirty(pos);
		}
	}

	private void markDirty(long pos) {
		if (this.level.getServer().isSameThread()) {
			this.dirty.add(pos);
		} else {
			ChunkDebug.LOGGER.warn("Tried marking dirty off-thread");
		}
	}

	public void reload() {

	}

	public record DirtyChunks(List<ChunkData> updated, LongList removed) { }
}
