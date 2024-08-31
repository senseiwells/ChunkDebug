package me.senseiwells.chunkdebug.client.gui;

import net.minecraft.world.level.ChunkPos;

import java.util.Objects;

public class ChunkSelection {
	public final int minX;
	public final int minZ;
	public final int maxX;
	public final int maxZ;

	public ChunkSelection(ChunkPos first, ChunkPos second) {
		this.minX = Math.min(first.x, second.x);
		this.minZ = Math.min(first.z, second.z);
		this.maxX = Math.max(first.x, second.x);
		this.maxZ = Math.max(first.z, second.z);
	}

	public int sizeX() {
		return this.maxX - this.minX + 1;
	}

	public int sizeZ() {
		return this.maxZ - this.minZ + 1;
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
}
