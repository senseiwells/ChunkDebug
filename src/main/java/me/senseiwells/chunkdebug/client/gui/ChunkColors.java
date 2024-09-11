package me.senseiwells.chunkdebug.client.gui;

import me.senseiwells.chunkdebug.client.mixins.LevelLoadingScreenAccessor;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChunkColors {
	public static int calculateChunkColor(
		FullChunkStatus status,
		@Nullable ChunkStatus stage,
		List<Ticket<?>> tickets,
		boolean unloading
	) {
		if (unloading) {
			return 0xFF0000;
		}
		if (stage != null && stage != ChunkStatus.FULL) {
			return LevelLoadingScreenAccessor.getStageColorMap().getInt(stage);
		}

		for (Ticket<?> ticket : tickets) {
			int color = calculateTicketTypeColor(ticket.getType());
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

	private static int calculateTicketTypeColor(TicketType<?> type) {
		if (type == TicketType.START) {
			return 0xBFFF00;
		}
		if (type == TicketType.DRAGON) {
			return 0xCC00CC;
		}
		if (type == TicketType.FORCED) {
			return 0x336FFF;
		}
		if (type == TicketType.PORTAL) {
			return 0x472483;
		}
		if (type == TicketType.POST_TELEPORT) {
			return 0xFF6600;
		}
		if (type == TicketType.ENDER_PEARL) {
			return 0x31D1B8;
		}
		return -1;
	}
}
