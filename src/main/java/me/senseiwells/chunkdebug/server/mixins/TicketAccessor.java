package me.senseiwells.chunkdebug.server.mixins;

import net.minecraft.server.level.Ticket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Ticket.class)
public interface TicketAccessor {
	@Accessor("ticksLeft")
	long accessTicksLeft();
}
