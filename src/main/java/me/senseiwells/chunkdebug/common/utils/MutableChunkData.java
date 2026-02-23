package me.senseiwells.chunkdebug.common.utils;

import com.google.common.collect.ImmutableList;
import me.senseiwells.chunkdebug.server.mixins.TicketAccessor;
import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MutableChunkData implements ChunkData {
	private final ChunkPos position;
	@Nullable private ChunkStatus stage;
	private List<Ticket> tickets;

	private int statusLevel;
	private int tickingStatusLevel;
	private boolean unloading;

	public MutableChunkData(
		ChunkPos position,
		@Nullable ChunkStatus stage,
		List<Ticket> tickets,
		int statusLevel,
		int tickingStatusLevel,
		boolean unloading
	) {
		this.position = position;
		this.stage = stage;

		this.statusLevel = statusLevel;
		this.tickingStatusLevel = tickingStatusLevel;
		this.unloading = unloading;

		// We need to make a copy of the tickets
		this.updateTickets(tickets);
	}

	@Override
	public ChunkPos position() {
		return this.position;
	}

	@Nullable
	@Override
	public ChunkStatus stage() {
		return this.stage;
	}

	@Override
	public int statusLevel() {
		return this.statusLevel;
	}

	@Override
	public int tickingStatusLevel() {
		return this.tickingStatusLevel;
	}

	@Override
	public boolean unloading() {
		return this.unloading;
	}

	public void updateStage(ChunkStatus stage) {
		this.stage = stage;
	}

	public void updateTickets(List<Ticket> tickets) {
		this.tickets = ImmutableList.copyOf(tickets);
	}

	@SuppressWarnings("unused")
	public void updateStatusLevel(int statusLevel) {
		this.statusLevel = statusLevel;
	}

	public void updateTickingStatusLevel(int statusLevel) {
		this.tickingStatusLevel = statusLevel;
	}

	public void updateUnloading(boolean unloading) {
		this.unloading = unloading;
	}

	public ImmutableChunkData immutable() {
		return new ImmutableChunkData(
			this.position,
			this.stage,
			this.immutableTickets(),
			this.statusLevel,
			this.tickingStatusLevel,
			this.unloading
		);
	}

	private List<ImmutableChunkData.Ticket> immutableTickets() {
		ImmutableChunkData.Ticket[] copied = new ImmutableChunkData.Ticket[this.tickets.size()];
		for (int i = 0; i < this.tickets.size(); i++) {
			Ticket ticket = this.tickets.get(i);
			copied[i] = new ImmutableChunkData.Ticket(
				ticket.getType(),
				ticket.getTicketLevel(),
				((TicketAccessor) ticket).accessTicksLeft()
			);
		}
		return List.of(copied);
	}
}