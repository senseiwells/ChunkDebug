package me.senseiwells.chunkdebug.client.gui;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.senseiwells.chunkdebug.client.mixins.LevelLoadingScreenAccessor;
import me.senseiwells.chunkdebug.common.utils.ImmutableChunkData.Ticket;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChunkColors {
    private static final Object2IntOpenHashMap<TicketType> TICKET_COLORS = new Object2IntOpenHashMap<>();

    static {
        registerTicketTypeColor(TicketType.PLAYER_SPAWN, 0xBFFF00);
        registerTicketTypeColor(TicketType.SPAWN_SEARCH, 0xBFFF00);
        registerTicketTypeColor(TicketType.DRAGON, 0xCC00CC);
        registerTicketTypeColor(TicketType.FORCED, 0x336FFF);
        registerTicketTypeColor(TicketType.PORTAL, 0x472483);
        registerTicketTypeColor(TicketType.ENDER_PEARL, 0x31D1B8);

        TICKET_COLORS.defaultReturnValue(-1);
    }

    public static int calculateChunkColor(
        FullChunkStatus status,
        @Nullable ChunkStatus stage,
        List<Ticket> tickets,
        boolean unloading
    ) {
        if (unloading) {
            return 0xFF0000;
        }
        if (stage != null && stage != ChunkStatus.FULL) {
            return LevelLoadingScreenAccessor.getStageColorMap().getInt(stage);
        }

        for (Ticket ticket : tickets) {
            int color = TICKET_COLORS.getInt(ticket.type());
            if (color != -1) {
                return color;
            }
        }
        return switch (status) {
            case INACCESSIBLE -> 0x404040;
            case FULL -> 0x4FC3F7;
            case BLOCK_TICKING -> 0xFFA219;
            case ENTITY_TICKING -> 0x198C19;
        };
    }

    public static void registerTicketTypeColor(TicketType type, int color) {
        TICKET_COLORS.put(type, color);
    }
}
