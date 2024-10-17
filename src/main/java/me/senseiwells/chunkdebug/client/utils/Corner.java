package me.senseiwells.chunkdebug.client.utils;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum Corner implements StringRepresentable {
	TOP_LEFT,
	TOP_RIGHT,
	BOTTOM_LEFT,
	BOTTOM_RIGHT;

	public static final Codec<Corner> CODEC = StringRepresentable.fromEnum(Corner::values);

	public Corner next() {
		return switch (this) {
			case TOP_LEFT -> TOP_RIGHT;
			case TOP_RIGHT -> BOTTOM_RIGHT;
			case BOTTOM_RIGHT -> BOTTOM_LEFT;
			case BOTTOM_LEFT -> TOP_LEFT;
		};
	}

	public Corner previous() {
		return switch (this) {
			case TOP_LEFT -> BOTTOM_LEFT;
			case TOP_RIGHT -> TOP_LEFT;
			case BOTTOM_RIGHT -> TOP_RIGHT;
			case BOTTOM_LEFT -> BOTTOM_RIGHT;
		};
	}

	public boolean isTop() {
		return this == TOP_LEFT || this == TOP_RIGHT;
	}

	public boolean isLeft() {
		return this == TOP_LEFT || this == BOTTOM_LEFT;
	}

	@NotNull
	@Override
	public String getSerializedName() {
		return this.name().toLowerCase(Locale.ROOT);
	}
}
