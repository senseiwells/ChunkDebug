package me.senseiwells.chunkdebug.client.gui;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongStack;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public class ChunkClusters {
	private final List<LongSet> groups;
	private final LongSet chunks;

	public ChunkClusters() {
		this.groups = new ArrayList<>();
		this.chunks = new LongOpenHashSet();
	}

	public void add(long position) {
		if (!this.chunks.add(position)) {
			return;
		}

		long[] directions = getOffsets(position);
		long up = directions[0];
		long down = directions[1];
		long right = directions[2];
		long left = directions[3];

		List<LongSet> nearby = new ArrayList<>(4);
		for (LongSet positions : this.groups) {
			if (positions.contains(up) || positions.contains(down) || positions.contains(right) || positions.contains(left)) {
				nearby.add(positions);
			}
		}

		if (nearby.isEmpty()) {
			LongSet set = new LongOpenHashSet();
			this.groups.add(set);
			set.add(position);
			return;
		}
		if (nearby.size() == 1) {
			nearby.getFirst().add(position);
			return;
		}

		// Our chunk was between two borders - find the largest
		LongSet largest = nearby.getFirst();
		for (int i = 1; i < nearby.size(); i++) {
			LongSet next = nearby.get(i);
			if (largest.size() < next.size()) {
				largest = next;
			}
		}
		largest.add(position);

		// Merge others into the largest
		for (LongSet set : nearby) {
			if (set != largest) {
				largest.addAll(set);
				this.groups.remove(set);
			}
		}
	}

	public void remove(long position) {
		if (!this.chunks.remove(position)) {
			return;
		}

		for (LongSet group : this.groups) {
			if (group.remove(position)) {
				// When we remove a group, it may split that group up
				this.groups.remove(group);
				if (group.isEmpty()) {
					return;
				}
				// We just completely recalculate the group
				this.groups.addAll(search(position, group));
				return;
			}
		}
	}

	public int count() {
		return this.groups.size();
	}

	public LongSet getCluster(int index) {
		return this.groups.get(index);
	}

	// We search around a position splitting into groups
	private static List<LongSet> search(long origin, LongSet originGroup) {
		long[] directions = getOffsets(origin);
		List<LongSet> groups = new ArrayList<>(4);

		for (long direction : directions) {
			if (!originGroup.contains(direction)) {
				continue;
			}
			boolean grouped = false;
			for (LongSet group : groups) {
				if (group.contains(direction)) {
					grouped = true;
					break;
				}
			}
			if (!grouped) {
				LongSet found = new LongOpenHashSet();
				searchFrom(direction, originGroup, new LongOpenHashSet(), found);
				if (!found.isEmpty()) {
					groups.add(found);
				}
			}
		}
		return groups;
	}

	private static void searchFrom(long position, LongSet group, LongSet checked, LongSet found) {
		LongStack stack = new LongArrayList();
		stack.push(position);

		while (!stack.isEmpty()) {
			long current = stack.popLong();
			long[] directions = getOffsets(current);

			for (long direction : directions) {
				if (checked.add(direction) && group.contains(direction)) {
					found.add(direction);
					stack.push(direction);
				}
			}
		}
	}

	private static long[] getOffsets(long position) {
		int x = (int) position;
		int z = (int) (position >> 32);
		return new long[]{pack(x + 1, z), pack(x - 1, z), pack(x, z + 1), pack(x, z - 1)};
	}

	private static long pack(int x, int z) {
		return ChunkPos.asLong(x, z);
	}
}
