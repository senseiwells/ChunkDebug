package me.senseiwells.chunkdebug.server.mixins;

import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.SimulationChunkTracker;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DistanceManager.class)
public interface DistanceManagerAccessor {
	@Accessor("ticketStorage")
	TicketStorage getTicketsStorage();

	@Accessor("simulationChunkTracker")
	SimulationChunkTracker getSimulationChunkTracker();
}
