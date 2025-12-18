package me.senseiwells.chunkdebug.server.mixins;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.senseiwells.chunkdebug.server.tracker.ChunkDebugTracker;
import me.senseiwells.chunkdebug.server.tracker.MutableChunkDebugTrackerHolder;
import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.TicketStorage;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(TicketStorage.class)
public class TicketStorageMixin implements MutableChunkDebugTrackerHolder {
    @Unique private ChunkDebugTracker chunkdebug$tracker;

    @Shadow @Final private Long2ObjectOpenHashMap<List<Ticket>> tickets;

    @Inject(
        method = "addTicket(JLnet/minecraft/server/level/Ticket;)Z",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;iterator()Ljava/util/Iterator;"
        )
    )
    private void onAddTicket(long pos, Ticket ticket, CallbackInfoReturnable<Boolean> cir, @Local List<Ticket> tickets) {
        if (this.chunkdebug$tracker != null) {
            this.chunkdebug$tracker.updateTickets(pos, tickets);
        }
    }

    @ModifyReturnValue(
        method = "removeTicket(JLnet/minecraft/server/level/Ticket;)Z",
        at = @At("RETURN")
    )
    private boolean onRemoveTicket(boolean original, long pos) {
        if (original && this.chunkdebug$tracker != null) {
            this.chunkdebug$tracker.updateTickets(pos, this.tickets.getOrDefault(pos, List.of()));
        }
        return original;
    }

    @ModifyReceiver(
        method = "removeTicketIf",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap$Entry;getValue()Ljava/lang/Object;",
            ordinal = 3,
            remap = false
        )
    )
    private Long2ObjectMap.Entry<List<Ticket>> onRemoveTicketIf(Long2ObjectMap.Entry<List<Ticket>> entry) {
        if (this.chunkdebug$tracker != null) {
            this.chunkdebug$tracker.updateTickets(entry.getLongKey(), entry.getValue());
        }
        return entry;
    }

    @Override
    public void chunkdebug$setTracker(@NonNull ChunkDebugTracker tracker) {
        this.chunkdebug$tracker = tracker;
    }

    @NonNull
    @Override
    public ChunkDebugTracker chunkdebug$getTracker() {
        return this.chunkdebug$tracker;
    }
}
