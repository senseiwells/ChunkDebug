package me.senseiwells.chunkdebug.common.utils;

import com.google.common.collect.HashBiMap;
import io.netty.buffer.ByteBuf;
import me.senseiwells.chunkdebug.ChunkDebug;
import me.senseiwells.chunkdebug.common.utils.ImmutableChunkData.Ticket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.*;

public class ExtraStreamCodecs {
	private static final Identifier UNREGISTERED = ChunkDebug.id("unregistered");

	public static final StreamCodec<RegistryFriendlyByteBuf, ResourceKey<Level>> DIMENSION = StreamCodec.of(ExtraStreamCodecs::encodeDimension, ExtraStreamCodecs::decodeDimension);
	public static final StreamCodec<RegistryFriendlyByteBuf, List<ResourceKey<Level>>> DIMENSIONS = ByteBufCodecs.<RegistryFriendlyByteBuf, ResourceKey<Level>>list().apply(DIMENSION);
	public static final StreamCodec<ByteBuf, Optional<ChunkStatus>> OPTIONAL_CHUNK_STATUS = ByteBufCodecs.fromCodec(ExtraCodecs.OPTIONAL_CHUNK_STATUS);
	public static final StreamCodec<FriendlyByteBuf, Ticket> TICKET = StreamCodec.of(ExtraStreamCodecs::encodeTicket, ExtraStreamCodecs::decodeTicket);
	public static final StreamCodec<FriendlyByteBuf, List<Ticket>> TICKETS = ByteBufCodecs.<FriendlyByteBuf, Ticket>list().apply(TICKET);

	private static final HashBiMap<TicketType, Identifier> CUSTOM_TICKET_TYPES = HashBiMap.create();

	public static String getTicketTypeAsString(TicketType type) {
		Identifier location = CUSTOM_TICKET_TYPES.get(type);
		if (location != null) {
			return location.toString();
		}
		return Util.getRegisteredName(BuiltInRegistries.TICKET_TYPE, type);
	}

	private static void encodeDimension(RegistryFriendlyByteBuf buf, ResourceKey<Level> dimension) {
		buf.writeResourceKey(dimension);
	}

	private static ResourceKey<Level> decodeDimension(RegistryFriendlyByteBuf buf) {
		return buf.readResourceKey(Registries.DIMENSION);
	}

	private static void encodeTicket(FriendlyByteBuf buf, Ticket ticket) {
		Identifier id = BuiltInRegistries.TICKET_TYPE.getKey(ticket.type());
		if (id == null) {
			id = UNREGISTERED;
		}
		buf.writeIdentifier(id);
		buf.writeInt((int) ticket.ticksLeft());
		buf.writeInt(ticket.ticketLevel());
	}

	private static Ticket decodeTicket(FriendlyByteBuf buf) {
		TicketType type;
		Identifier id = buf.readIdentifier();
		Optional<TicketType> optional = BuiltInRegistries.TICKET_TYPE.getOptional(id);
		if (optional.isEmpty()) {
			type = CUSTOM_TICKET_TYPES.inverse().get(id);
			if (type == null) {
				type = new TicketType(0, TicketType.FLAG_PERSIST | TicketType.FLAG_LOADING);
				CUSTOM_TICKET_TYPES.put(type, id);
			}
		} else {
			type = optional.get();
		}

		int ticksRemaining = buf.readInt();
		int ticketLevel = buf.readInt();
        return new Ticket(type, ticketLevel, ticksRemaining);
	}
}
