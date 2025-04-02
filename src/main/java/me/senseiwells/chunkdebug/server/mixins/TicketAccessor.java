package me.senseiwells.chunkdebug.server.mixins;

import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Ticket.class)
public interface TicketAccessor {
	@Accessor("ticksLeft")
	long getRemainingTicks();

	@Invoker("<init>")
 	static Ticket construct(TicketType type, int ticketLevel, long ticksRemaining) {
		throw new AssertionError();
	}
}
