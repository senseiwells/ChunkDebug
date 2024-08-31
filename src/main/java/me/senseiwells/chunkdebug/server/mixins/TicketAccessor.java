package me.senseiwells.chunkdebug.server.mixins;

import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Ticket.class)
public interface TicketAccessor {
	@Accessor("createdTick")
	long getTickCreated();

	@Accessor("createdTick")
	void setTickCreated(long tick);

	@Invoker("<init>")
 	static <T> Ticket<T> construct(TicketType<T> type, int ticketLevel, T key) {
		throw new AssertionError();
	}
}
