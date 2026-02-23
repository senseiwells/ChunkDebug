package me.senseiwells.chunkdebug.server.tracker;

import it.unimi.dsi.fastutil.longs.*;
import me.senseiwells.chunkdebug.common.utils.ImmutableChunkData;
import me.senseiwells.chunkdebug.common.utils.MutableChunkData;
import me.senseiwells.chunkdebug.server.holder.ChunkDataSupplier;
import me.senseiwells.chunkdebug.server.holder.ChunkHolderSupplier;
import me.senseiwells.chunkdebug.server.utils.LevelUtils;
import net.minecraft.server.level.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChunkDebugTracker {
	private final Long2ObjectMap<MutableChunkData> chunks = new Long2ObjectOpenHashMap<>();
	private final LongSet dirty = new LongOpenHashSet();

	private final Long2ObjectMap<ChunkStatus> stages = new Long2ObjectOpenHashMap<>();

	private final ServerLevel level;

	public ChunkDebugTracker(ServerLevel level) {
		this.level = level;
	}

	public Collection<ImmutableChunkData> getChunks() {
		this.checkSameThread();
		return this.chunks.values().stream().map(MutableChunkData::immutable).toList();
	}

	public DirtyChunks getDirtyChunks() {
		this.checkSameThread();
		List<ImmutableChunkData> updated = new ArrayList<>(this.dirty.size());
		LongList removed = new LongArrayList();
		LongIterator iter = this.dirty.iterator();
		while (iter.hasNext()) {
			long pos = iter.nextLong();
			if (this.chunks.containsKey(pos)) {
				MutableChunkData data = this.chunks.get(pos);
				updated.add(data.immutable());
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
				if (this.chunks.containsKey(pos)) {
					MutableChunkData data = this.chunks.get(pos);
					ChunkStatus stage = entry.getValue();
					if (data.stage() != stage) {
						data.updateStage(stage);
						this.markDirty(pos);
					}
				}
			}

			this.stages.clear();
		}
	}

	public void refresh() {
		this.checkSameThread();

		this.chunks.clear();
		this.dirty.clear();
		synchronized (this.stages) {
			this.stages.clear();
		}

		ChunkMap chunkMap = this.level.getChunkSource().chunkMap;
		((ChunkHolderSupplier) chunkMap).chunkdebug$forEachChunkHolder(holder -> {
			this.set(((ChunkDataSupplier) holder).chunkdebug$getChunkData(chunkMap));
		});
	}

	public void set(MutableChunkData data) {
		long pos = data.position().toLong();
		if (this.markDirty(pos)) {
			this.chunks.put(pos, data);
		}
	}

	public void unload(long pos) {
		if (this.markDirty(pos)) {
			this.chunks.remove(pos);
		}
	}

	public void updateStage(long pos, ChunkStatus stage) {
		synchronized (this.stages) {
			this.stages.put(pos, stage);
		}
	}

	public void updateTickets(long pos, List<Ticket> tickets) {
		if (this.chunks.containsKey(pos)) {
			MutableChunkData data = this.chunks.get(pos);
            data.updateTickets(tickets);
            this.markDirty(pos);
        }
	}

	public void updateTickingStatusLevel(long pos, int level) {
		if (this.chunks.containsKey(pos)) {
			MutableChunkData data = this.chunks.get(pos);
            data.updateTickingStatusLevel(level);
            this.markDirty(pos);
        }
	}

	public void updateUnloading(long pos, boolean unloading) {
		if (this.chunks.containsKey(pos)) {
			MutableChunkData data = this.chunks.get(pos);
            data.updateUnloading(unloading);
            this.markDirty(pos);
        }
	}

	private boolean markDirty(long pos) {
		// For compatibility with c2me - if the server is stopping, we don't care anyway
		if (!this.level.getServer().isStopped()) {
			this.checkSameThread();
			this.dirty.add(pos);
			return true;
		}
		return false;
	}

	private void checkSameThread() {
		if (!LevelUtils.isSameThread(this.level)) {
			// We just throw an exception, otherwise we risk a
			// race condition at some random undetermined point
			throw new IllegalStateException("Tried using ChunkDebugTracker off level thread");
		}
	}

	public record DirtyChunks(List<ImmutableChunkData> updated, LongList removed) { }
}
