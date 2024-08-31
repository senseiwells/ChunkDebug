package me.senseiwells.chunkdebug.server.mixins;

import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TickingTracker;
import net.minecraft.util.SortedArraySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DistanceManager.class)
public interface DistanceManagerAccessor {
	@Invoker("getTickets")
	SortedArraySet<Ticket<?>> getTicketsFor(long chunkPos);

	@Accessor("ticketTickCounter")
	void setTickCount(long ticks);

	@Accessor("tickingTicketsTracker")
	TickingTracker getTickingTracker();
}
