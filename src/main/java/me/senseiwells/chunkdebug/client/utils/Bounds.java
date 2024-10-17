package me.senseiwells.chunkdebug.client.utils;

public record Bounds(int minX, int minY, int maxX, int maxY) {
	public boolean contains(double x, double y) {
		return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY;
	}

	public Bounds offset(int dx, int dy) {
		return new Bounds(this.minX + dx, this.minY + dy, this.maxX + dx, this.maxY + dy);
	}
}
