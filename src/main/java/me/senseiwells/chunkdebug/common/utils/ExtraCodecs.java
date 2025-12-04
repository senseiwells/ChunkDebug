package me.senseiwells.chunkdebug.common.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.util.Unit;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.Optional;

public class ExtraCodecs {
    public static final Codec<Optional<ChunkStatus>> OPTIONAL_CHUNK_STATUS = optional(ChunkStatus.CODEC)
        .orElse(Optional.empty());

    public static <A> Codec<Optional<A>> optional(Codec<A> codec) {
        return Codec.either(codec, Unit.CODEC).xmap(
            either -> either.map(Optional::of, unit -> Optional.empty()),
            optional -> optional.map(Either::<A, Unit>left).orElseGet(() -> Either.right(Unit.INSTANCE))
        );
    }
}
