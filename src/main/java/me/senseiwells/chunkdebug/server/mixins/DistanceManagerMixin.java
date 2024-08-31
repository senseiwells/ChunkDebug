package me.senseiwells.chunkdebug.server.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTrackerHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.util.SortedArraySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DistanceManager.class)
public class DistanceManagerMixin {
	@Inject(
		method = {
			"addTicket(JLnet/minecraft/server/level/Ticket;)V",
			"removeTicket(JLnet/minecraft/server/level/Ticket;)V"
		},
		at = @At("TAIL")
	)
	private void onAddTicket(long chunkPos, Ticket<?> ticket, CallbackInfo ci, @Local SortedArraySet<Ticket<?>> tickets) {
		if ((Object) this instanceof ChunkMap.DistanceManager manager) {
			ChunkMap chunks = ((ChunkDistanceManagerAccessor) manager).getChunkMap();
			ServerLevel level = ((ChunkMapAccessor) chunks).getLevel();
			ChunkDebugTracker tracker = ((ChunkDebugTrackerHolder) level).chunkdebug$getTracker();
			tracker.updateTickets(chunkPos, tickets);
		}
	}
}
