package me.senseiwells.chunkdebug.server.tracker;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import me.senseiwells.chunkdebug.common.utils.ChunkData;
import net.minecraft.server.level.*;

import java.util.Collection;
import java.util.Collections;

public class ChunkDebugTracker {
	private final Long2ObjectMap<ChunkData> chunks = new Long2ObjectOpenHashMap<>();
	private Long2ObjectMap<ChunkData> updates = new Long2ObjectOpenHashMap<>();

	private final ServerLevel level;

	public ChunkDebugTracker(ServerLevel level) {
		this.level = level;
	}

	public Collection<ChunkData> getChunks() {
		return this.chunks.values();
	}

	public Collection<ChunkData> getChunkUpdates() {
		Collection<ChunkData> dirty = this.updates.values();
		this.updates = new Long2ObjectOpenHashMap<>();
		return dirty;
	}
	
	public void tick() {

	}

	public void update(ChunkData data) {
		long pos = data.position().toLong();
		this.updates.put(pos, data);
		if (data.status() != FullChunkStatus.INACCESSIBLE) {
			this.chunks.put(pos, data);
		} else {
			this.chunks.remove(pos, data);
		}
	}

	public void reload() {

	}
}
