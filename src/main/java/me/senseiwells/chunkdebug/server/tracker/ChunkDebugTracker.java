package me.senseiwells.chunkdebug.server.tracker;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import me.senseiwells.chunkdebug.ChunkDebug;
import me.senseiwells.chunkdebug.common.utils.ChunkData;
import me.senseiwells.chunkdebug.server.mixins.ChunkMapAccessor;
import me.senseiwells.chunkdebug.server.mixins.DistanceManagerAccessor;
import net.minecraft.server.level.*;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
		// LinkedList<ChunkData> updated = new LinkedList<>();
		//
		// ChunkMap chunks = this.level.getChunkSource().chunkMap;
		// DistanceManagerAccessor manager = (DistanceManagerAccessor) chunks.getDistanceManager();
		// Long2ObjectMap<ChunkHolder> holders = ((ChunkMapAccessor) chunks).getChunkHolderMap();
		// for (ChunkData data : this.chunks.values()) {
		// 	ChunkHolder holder = holders.get(data.position().toLong());
		//
		// 	boolean dirty = false;
		// 	int statusLevel = holder.getTicketLevel();
		// 	int tickingStatusLevel =  manager.getTickingTracker().getLevel(data.position());
		// 	ChunkStatus stage = holder.getPersistedStatus();
		// 	stage = stage == null ? ChunkStatus.EMPTY : stage;
		// 	List<Ticket<?>> tickets = ImmutableList.copyOf(manager.getTicketsFor(data.position().toLong()));
		// 	if (statusLevel != data.statusLevel()) {
		// 		dirty = true;
		// 	}
		// 	if (tickingStatusLevel != data.tickingStatusLevel()) {
		// 		dirty = true;
		// 	}
		// 	if (stage != data.stage()) {
		// 		dirty = true;
		// 	}
		// 	if (!tickets.equals(data.tickets())) {
		// 		dirty = true;
		// 	}
		// 	if (dirty) {
		// 		updated.add(new ChunkData(data.position(), statusLevel, tickingStatusLevel, stage, tickets));
		// 	}
		// }
		//
		// for (ChunkData data : updated) {
		// 	this.update(data);
		// }
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

	public void updateTickets(long pos, SortedArraySet<Ticket<?>> tickets) {
		ChunkData data = this.chunks.get(pos);
		if (data != null) {
			this.update(new ChunkData(data.position(), data.statusLevel(), data.tickingStatusLevel(), data.stage(), tickets));
		}
	}

	public void reload() {

	}
}
