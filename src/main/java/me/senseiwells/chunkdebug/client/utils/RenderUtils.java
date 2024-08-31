package me.senseiwells.chunkdebug.client.utils;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

public class RenderUtils {
	public static void renderOutline(GuiGraphics graphics, float x, float y, float width, float height, float thickness, int color) {
		fill(graphics, x, y, x + width, y + thickness, color);
		fill(graphics, x, y + thickness, x + thickness, y + height - thickness, color);
		fill(graphics, x + width - thickness, y + thickness, x + width, y + height - thickness, color);
		fill(graphics, x, y + height - thickness, x + width, y + height, color);
	}

	public static void fill(GuiGraphics graphics, float minX, float minY, float maxX, float maxY, int color) {
		fill(graphics, RenderType.gui(), minX, minY, maxX, maxY, color);
	}

	public static void fill(GuiGraphics graphics, RenderType type, float minX, float minY, float maxX, float maxY, int color) {
		Matrix4f matrix4f = graphics.pose().last().pose();
		VertexConsumer consumer = graphics.bufferSource().getBuffer(type);
		consumer.addVertex(matrix4f, minX, minY, 0.0F).setColor(color);
		consumer.addVertex(matrix4f, minX, maxY, 0.0F).setColor(color);
		consumer.addVertex(matrix4f, maxX, maxY, 0.0F).setColor(color);
		consumer.addVertex(matrix4f, maxX, minY, 0.0F).setColor(color);
		graphics.flush();
	}
}
