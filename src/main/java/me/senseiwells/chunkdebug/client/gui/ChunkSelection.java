package me.senseiwells.chunkdebug.client.gui;

import net.minecraft.world.level.ChunkPos;

import java.util.List;
import java.util.Objects;
import java.util.stream.LongStream;

public class ChunkSelection {
	public static final ChunkSelection EMPTY = new ChunkSelection(0, 0, 0, 0);

	public final int minX;
	public final int minZ;
	public final int maxX;
	public final int maxZ;

	public ChunkSelection(ChunkPos first, ChunkPos second) {
		this(first.x, first.z, second.x, second.z);
	}

	public ChunkSelection(int x1, int z1, int x2, int z2) {
		this.minX = Math.min(x1, x2);
		this.minZ = Math.min(z1, z2);
		this.maxX = Math.max(x1, x2);
		this.maxZ = Math.max(z1, z2);
	}

	public int sizeX() {
		return this.maxX - this.minX + 1;
	}

	public int sizeZ() {
		return this.maxZ - this.minZ + 1;
	}

	public boolean isSingleChunk() {
		return this.minX == this.maxX && this.minZ == this.maxZ;
	}

	public ChunkPos getMinChunkPos() {
		return new ChunkPos(this.minX, this.minZ);
	}

	public ChunkPos getCenterChunkPos() {
		return new ChunkPos((this.minX + this.maxX) / 2, (this.minZ + this.maxZ) / 2);
	}

	public ChunkPos getMaxChunkPos() {
		return new ChunkPos(this.maxX, this.maxZ);
	}

	public LongStream stream() {
		return LongStream.rangeClosed(this.minX, this.maxX).flatMap(x -> {
			return LongStream.rangeClosed(this.minZ, this.maxZ).map(z -> ChunkPos.asLong((int) x, (int) z));
		});
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ChunkSelection that)) {
			return false;
		}

		return this.minX == that.minX &&
			this.minZ == that.minZ &&
			this.maxX == that.maxX &&
			this.maxZ == that.maxZ;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.minX, this.minZ, this.maxX, this.maxZ);
	}

	public static ChunkSelection fromPositions(List<ChunkPos> positions) {
		if (positions.isEmpty()) {
			return ChunkSelection.EMPTY;
		}
		int minX = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (ChunkPos pos : positions) {
			minX = Math.min(minX, pos.x);
			minZ = Math.min(minZ, pos.z);
			maxX = Math.max(maxX, pos.x);
			maxZ = Math.max(maxZ, pos.z);
		}
		return new ChunkSelection(minX, minZ, maxX, maxZ);
	}
}
