package me.senseiwells.chunkdebug.common.utils;

import com.google.common.collect.HashBiMap;
import me.senseiwells.chunkdebug.server.mixins.TicketAccessor;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.*;

public class ExtraStreamCodecs {
	private static final ResourceLocation UNREGISTERED = ResourceLocation.fromNamespaceAndPath("chunk-debug", "unregistered");

	public static final StreamCodec<RegistryFriendlyByteBuf, ResourceKey<Level>> DIMENSION = StreamCodec.of(ExtraStreamCodecs::encodeDimension, ExtraStreamCodecs::decodeDimension);
	public static final StreamCodec<RegistryFriendlyByteBuf, List<ResourceKey<Level>>> DIMENSIONS = ByteBufCodecs.<RegistryFriendlyByteBuf, ResourceKey<Level>>list().apply(DIMENSION);
	public static final StreamCodec<RegistryFriendlyByteBuf, Holder<ChunkStatus>> CHUNK_STATUS = ByteBufCodecs.holderRegistry(Registries.CHUNK_STATUS);
	public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Holder<ChunkStatus>>> OPTIONAL_CHUNK_STATUS = ByteBufCodecs.optional(CHUNK_STATUS);
	public static final StreamCodec<FriendlyByteBuf, Ticket> TICKET = StreamCodec.of(ExtraStreamCodecs::encodeTicket, ExtraStreamCodecs::decodeTicket);
	public static final StreamCodec<FriendlyByteBuf, List<Ticket>> TICKETS = ByteBufCodecs.<FriendlyByteBuf, Ticket>list().apply(TICKET);

	private static final HashBiMap<TicketType, ResourceLocation> CUSTOM_TICKET_TYPES = HashBiMap.create();

	public static String getTicketTypeAsString(TicketType type) {
		ResourceLocation location = CUSTOM_TICKET_TYPES.get(type);
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
		ResourceLocation location = BuiltInRegistries.TICKET_TYPE.getKey(ticket.getType());
		if (location == null) {
			location = UNREGISTERED;
		}
		buf.writeResourceLocation(location);
		buf.writeInt((int) ((TicketAccessor) ticket).getRemainingTicks());
		buf.writeInt(ticket.getTicketLevel());
	}

	private static Ticket decodeTicket(FriendlyByteBuf buf) {
		TicketType type;
		ResourceLocation location = buf.readResourceLocation();
		Optional<TicketType> optional = BuiltInRegistries.TICKET_TYPE.getOptional(location);
		if (optional.isEmpty()) {
			type = CUSTOM_TICKET_TYPES.inverse().get(location);
			if (type == null) {
				type = new TicketType(0, TicketType.FLAG_PERSIST | TicketType.FLAG_LOADING);
				CUSTOM_TICKET_TYPES.put(type, location);
			}
		} else {
			type = optional.get();
		}

		int ticksRemaining = buf.readInt();
		int ticketLevel = buf.readInt();
        return TicketAccessor.construct(type, ticketLevel, ticksRemaining);
	}
}
