package me.senseiwells.chunkdebug.client.gui;

import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.senseiwells.chunkdebug.common.utils.ChunkData;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public record ChunkSelectionInfo(
	Component title,
	List<List<Component>> sections
) {
	public int getMaxWidth(Font font) {
		int width = 0;
		for (Component component : Iterables.concat(this.sections)) {
			width = Math.max(width, font.width(component));
		}
		return Math.max(width, font.width(this.title));
	}

	public static ChunkSelectionInfo create(ChunkSelection selection, Long2ObjectMap<ChunkData> chunks) {
		Component title;
		List<Component> location = new ArrayList<>();
		List<Component> status = new ArrayList<>();
		List<Component> tickets = new ArrayList<>();
		if (selection.isSingleChunk()) {
			ChunkPos pos = selection.getMinChunkPos();
			title = Component.literal("Selected Chunk Breakdown");
			location.add(Component.literal("Location: " + pos));

			ChunkData data = chunks.get(pos.toLong());
			if (data != null) {
				status.add(Component.literal("Status: ").append(prettify(data.status())));
				status.add(Component.literal("Status Level: " + data.statusLevel()));
				status.add(Component.literal("Ticking Status Level: " + data.tickingStatusLevel()));

				if (!data.tickets().isEmpty()) {
					tickets.add(Component.literal("Tickets:"));
					for (Ticket<?> ticket : data.tickets()) {
						tickets.add(
							Component.literal(" Type: ")
								.append(prettify(ticket.getType()))
								.append(", Level: " + ticket.getTicketLevel())
						);
					}
				}
			} else {
				status.add(Component.literal("Status: Unloaded"));
			}
		} else {
			title = Component.literal("Selected Chunks Breakdown");
		}

		return new ChunkSelectionInfo(title, List.of(location, status, tickets));
	}

	private static Component prettify(FullChunkStatus status) {
		return switch (status) {
			case INACCESSIBLE -> Component.literal("Unloaded");
			case FULL -> Component.literal("Border");
			case BLOCK_TICKING -> Component.literal("Lazy");
			case ENTITY_TICKING -> Component.literal("Entity Ticking");
		};
	}

	private static Component prettify(TicketType<?> status) {
		if (status == TicketType.START) {
			return Component.literal("Start");
		}
		if (status == TicketType.DRAGON) {
			return Component.literal("Dragon");
		}
		if (status == TicketType.PLAYER) {
			return Component.literal("Player");
		}
		if (status == TicketType.PORTAL) {
			return Component.literal("Portal");
		}
		if (status == TicketType.POST_TELEPORT) {
			return Component.literal("Post Teleport");
		}
		if (status == TicketType.FORCED) {
			return Component.literal("Forced");
		}
		return Component.literal("Unknown");
	}
}