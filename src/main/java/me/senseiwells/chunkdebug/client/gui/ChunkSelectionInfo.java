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
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.List;

public record ChunkSelectionInfo(
	Component title,
	List<List<Component>> sections
) {
	private static final int BLUE = 0x6CB4EE;
	private static final int YELLOW = 0xFFFD8D;

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
		List<Component> stages = new ArrayList<>();
		List<Component> unloading = new ArrayList<>();
		if (selection.isSingleChunk()) {
			ChunkPos pos = selection.getMinChunkPos();
			title = Component.translatable("chunk-debug.info.breakdown.chunk").withColor(YELLOW);
			location.add(Component.translatable("chunk-debug.info.location", prettify(pos)));

			ChunkData data = chunks.get(pos.toLong());
			if (data != null) {
				status.add(Component.translatable("chunk-debug.info.status", prettify(data.status())));
				status.add(Component.translatable("chunk-debug.info.status.level", prettify(data.statusLevel())));
				status.add(Component.translatable("chunk-debug.info.status.level.ticking", prettify(data.tickingStatusLevel())));

				if (!data.tickets().isEmpty()) {
					tickets.add(Component.translatable("chunk-debug.info.tickets"));
					for (Ticket<?> ticket : data.tickets()) {
						Component type = prettify(ticket.getType());
						Component level = prettify(ticket.getTicketLevel());
						tickets.add(Component.translatable("chunk-debug.info.tickets.details", type, level));
					}
				}

				ChunkStatus stage = data.stage();
				if (stage != null) {
					stages.add(Component.translatable("chunk-debug.info.stage", prettify(stage)));
				}

				unloading.add(Component.translatable("chunk-debug.info.unloading", prettify(data.unloading())));
			} else {
				Component unloaded = Component.translatable("chunk-debug.status.unloaded");
				status.add(Component.translatable("chunk-debug.info.status", unloaded));
			}
		} else {
			title = Component.translatable("chunk-debug.info.breakdown.chunks");
		}

		return new ChunkSelectionInfo(title, List.of(location, status, tickets, stages, unloading));
	}

	private static Component prettify(ChunkPos pos) {
		return Component.literal(pos.toString()).withColor(BLUE);
	}

	private static Component prettify(int statusLevel) {
		return Component.literal(Integer.toString(statusLevel)).withColor(BLUE);
	}

	private static Component prettify(FullChunkStatus status) {
		return Component.translatable("chunk-debug.status." + status.name().toLowerCase()).withColor(BLUE);
	}

	private static Component prettify(TicketType<?> type) {
		return Component.translatable("chunk-debug.ticket.type." + type).withColor(BLUE);
	}

	private static Component prettify(ChunkStatus stage) {
		return Component.translatable("chunk-debug.stage." + stage.getName()).withColor(BLUE);
	}

	private static Component prettify(boolean bool) {
		return Component.translatable("chunk-debug.boolean." + bool).withColor(BLUE);
	}
}