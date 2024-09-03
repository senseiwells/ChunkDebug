package me.senseiwells.chunkdebug.client.gui;

import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.senseiwells.chunkdebug.common.utils.ChunkData;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static me.senseiwells.chunkdebug.client.utils.RenderUtils.HL;

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
		List<Component> stages = new ArrayList<>();
		List<Component> unloading = new ArrayList<>();
		if (selection.isSingleChunk()) {
			ChunkPos pos = selection.getCenterChunkPos();
			title = Component.translatable("chunk-debug.info.breakdown.chunk").withColor(HL);
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
			title = Component.translatable("chunk-debug.info.breakdown.chunks").withColor(HL);

			Component range = Component.empty()
				.append(prettify(selection.getMinChunkPos()))
				.append(" -> ")
				.append(prettify(selection.getMaxChunkPos()));
			location.add(Component.translatable("chunk-debug.info.location", range));

			Component area = Component.empty()
				.append(prettify(selection.sizeX()))
				.append("x")
				.append(prettify(selection.sizeZ()));
			location.add(Component.translatable("chunk-debug.info.area", area));

			int lowestLevel = Integer.MAX_VALUE;
			int highestLevel = Integer.MIN_VALUE;

			Object2IntLinkedOpenHashMap<FullChunkStatus> statuses = new Object2IntLinkedOpenHashMap<>();
			// We must order them correctly
			Arrays.stream(FullChunkStatus.values()).forEachOrdered(s -> statuses.put(s, 0));

			Object2IntOpenHashMap<TicketType<?>> types = new Object2IntOpenHashMap<>();

			List<ChunkData> selected = selection.stream().mapToObj(chunks::get).filter(Objects::nonNull).toList();
			for (ChunkData chunk : selected) {
				for (Ticket<?> ticket : chunk.tickets()) {
					types.addTo(ticket.getType(), 1);
				}
				statuses.addTo(chunk.status(), 1);
				if (chunk.statusLevel() > highestLevel) {
					highestLevel = chunk.statusLevel();
				}
				if (chunk.statusLevel() < lowestLevel) {
					lowestLevel =  chunk.statusLevel();
				}
			}

			status.add(Component.translatable("chunk-debug.info.status.range", prettify(lowestLevel), prettify(highestLevel)));
			tickets.add(Component.translatable("chunk-debug.info.status.distribution"));
			for (Object2IntMap.Entry<FullChunkStatus> entry : statuses.object2IntEntrySet()) {
				Component line = Component.empty()
					.append(prettify(entry.getKey()).withColor(0xFFFFFF))
					.append(": ")
					.append(prettify(entry.getIntValue()));
				tickets.add(line);
			}

			stages.add(Component.translatable("chunk-debug.info.tickets.distribution"));
			for (Object2IntMap.Entry<TicketType<?>> entry : types.object2IntEntrySet()) {
				Component line = Component.empty()
					.append(prettify(entry.getKey()).withColor(0xFFFFFF))
					.append(": ")
					.append(prettify(entry.getIntValue()));
				stages.add(line);
			}
		}

		return new ChunkSelectionInfo(title, List.of(location, status, tickets, stages, unloading));
	}

	private static MutableComponent prettify(ChunkPos pos) {
		return Component.literal(pos.toString()).withColor(HL);
	}

	private static MutableComponent prettify(int statusLevel) {
		return Component.literal(Integer.toString(statusLevel)).withColor(HL);
	}

	private static MutableComponent prettify(FullChunkStatus status) {
		return Component.translatable("chunk-debug.status." + status.name().toLowerCase()).withColor(HL);
	}

	private static MutableComponent prettify(TicketType<?> type) {
		return Component.translatable("chunk-debug.ticket.type." + type).withColor(HL);
	}

	private static MutableComponent prettify(ChunkStatus stage) {
		return Component.translatable("chunk-debug.stage." + stage.getName()).withColor(HL);
	}

	private static MutableComponent prettify(boolean bool) {
		return Component.translatable("chunk-debug.boolean." + bool).withColor(HL);
	}
}