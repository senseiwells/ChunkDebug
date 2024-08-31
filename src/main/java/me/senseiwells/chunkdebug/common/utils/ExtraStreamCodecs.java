package me.senseiwells.chunkdebug.common.utils;

import me.senseiwells.chunkdebug.common.network.StopWatchingPayload;
import me.senseiwells.chunkdebug.server.mixins.TicketAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.List;

public class ExtraStreamCodecs {
	public static final StreamCodec<RegistryFriendlyByteBuf, ResourceKey<Level>> DIMENSION = StreamCodec.of(ExtraStreamCodecs::encodeDimension, ExtraStreamCodecs::decodeDimension);
	public static final StreamCodec<RegistryFriendlyByteBuf, List<ResourceKey<Level>>> DIMENSIONS = ByteBufCodecs.<RegistryFriendlyByteBuf, ResourceKey<Level>>list().apply(DIMENSION);
	public static final StreamCodec<RegistryFriendlyByteBuf, Holder<ChunkStatus>> CHUNK_STATUS = ByteBufCodecs.holderRegistry(Registries.CHUNK_STATUS);
	public static final StreamCodec<FriendlyByteBuf, Ticket<?>> TICKET = StreamCodec.of(ExtraStreamCodecs::encodeTicket, ExtraStreamCodecs::decodeTicket);
	public static final StreamCodec<FriendlyByteBuf, List<Ticket<?>>> TICKETS = ByteBufCodecs.<FriendlyByteBuf, Ticket<?>>list().apply(TICKET);

	private static final List<TicketType<?>> TICKET_TYPES = getTicketTypes();

	private static void encodeDimension(RegistryFriendlyByteBuf buf, ResourceKey<Level> dimension) {
		buf.writeResourceKey(dimension);
	}

	private static ResourceKey<Level> decodeDimension(RegistryFriendlyByteBuf buf) {
		return buf.readResourceKey(Registries.DIMENSION);
	}

	private static void encodeTicket(FriendlyByteBuf buf, Ticket<?> ticket) {
		buf.writeByte(TICKET_TYPES.indexOf(ticket.getType()));
		buf.writeInt((int) ((TicketAccessor) (Object) ticket).getTickCreated());
		buf.writeInt(ticket.getTicketLevel());
	}

	private static Ticket<?> decodeTicket(FriendlyByteBuf buf) {
		TicketType<?> type = TICKET_TYPES.get(buf.readByte());
		int tickCreated = buf.readInt();
		int ticketLevel = buf.readInt();
		Ticket<?> ticket = TicketAccessor.construct(type, ticketLevel, null);
		((TicketAccessor) (Object) ticket).setTickCreated(tickCreated);
		return ticket;
	}

	private static List<TicketType<?>> getTicketTypes() {
		return List.of(
			TicketType.START,
			TicketType.DRAGON,
			TicketType.PLAYER,
			TicketType.FORCED,
			TicketType.POST_TELEPORT,
			TicketType.UNKNOWN
		);
	}
}
