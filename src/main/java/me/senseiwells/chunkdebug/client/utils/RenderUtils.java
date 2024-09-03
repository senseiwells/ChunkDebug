package me.senseiwells.chunkdebug.client.utils;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.senseiwells.chunkdebug.client.gui.widget.ArrowButton;
import me.senseiwells.chunkdebug.client.gui.widget.NamedButton;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

public class RenderUtils {
	public static final int HL_BG_LIGHT = 0xC8353B48;
	public static final int HL_BG_DARK = 0xC82D3436;

	public static final int BG_LIGHT = 0x55000000;
	public static final int BG_DARK = 0x55000000;

	public static final int HL = 0xC86CB4EE;

	public static void outline(GuiGraphics graphics, float x, float y, float width, float height, float thickness, int color) {
		fill(graphics, x, y, x + width, y + thickness, color);
		fill(graphics, x, y + thickness, x + thickness, y + height - thickness, color);
		fill(graphics, x + width - thickness, y + thickness, x + width, y + height - thickness, color);
		fill(graphics, x, y + height - thickness, x + width, y + height, color);
	}

	public static void fill(GuiGraphics graphics, float minX, float minY, float maxX, float maxY, int color) {
		Matrix4f matrix4f = graphics.pose().last().pose();
		VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());
		consumer.addVertex(matrix4f, minX, minY, 0.0F).setColor(color);
		consumer.addVertex(matrix4f, minX, maxY, 0.0F).setColor(color);
		consumer.addVertex(matrix4f, maxX, maxY, 0.0F).setColor(color);
		consumer.addVertex(matrix4f, maxX, minY, 0.0F).setColor(color);
		graphics.flush();
	}

	public static void triangle(
		GuiGraphics graphics,
		float minX,
		float minY,
		float maxX,
		float maxY,
		float angle,
		int color
	) {
		graphics.pose().pushPose();
		graphics.pose().rotateAround(Axis.ZP.rotationDegrees(angle), (minX + maxX) / 2, (minY + maxY) / 2, 0);
		Matrix4f matrix4f = graphics.pose().last().pose();
		VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());
		consumer.addVertex(matrix4f, minX, minY, 0.0F).setColor(color);
		consumer.addVertex(matrix4f, minX, maxY, 0.0F).setColor(color);
		consumer.addVertex(matrix4f, maxX, maxY - (maxY - minY) / 2, 0.0F).setColor(color);
		consumer.addVertex(matrix4f, minX, minY, 0.0F).setColor(color);
		graphics.flush();
		graphics.pose().popPose();
	}

	public static void options(
		GuiGraphics graphics,
		Font font,
		int minX,
		int maxX,
		int offsetY,
		int padding,
		Component name,
		ArrowButton left,
		ArrowButton right
	) {
		if (left.getWidth() != right.getWidth() || left.getHeight() != right.getHeight()) {
			throw new IllegalArgumentException("Expected buttons to be of the same size");
		}
		int buttonWidth = left.getWidth();
		int buttonHeight = left.getHeight();

		int offsetX = minX + padding;
		int offsetMaxX = maxX - padding - buttonWidth;
		int offsetMaxY = offsetY + buttonHeight;

		left.setPosition(offsetX, offsetY);
		right.setPosition(offsetMaxX, offsetY);

		offsetX += padding + buttonWidth;
		offsetMaxX -= padding;
		graphics.fill(offsetX, offsetY, offsetMaxX, offsetMaxY, BG_DARK);
		NamedButton.renderScrollingString(graphics, font, name, offsetX, offsetY, offsetMaxX, offsetMaxY);
	}

	public static void optionLeft(
		GuiGraphics graphics,
		Font font,
		int minX,
		int maxX,
		int offsetY,
		int padding,
		Component name,
		AbstractWidget toggle
	) {
		int buttonWidth = toggle.getWidth();
		int buttonHeight = toggle.getHeight();

		int offsetX = minX + padding;
		int offsetMaxX = maxX - padding;
		int offsetMaxY = offsetY + buttonHeight;

		toggle.setPosition(offsetX, offsetY);

		offsetX += padding + buttonWidth;
		graphics.fill(offsetX, offsetY, offsetMaxX, offsetMaxY, BG_DARK);
		graphics.drawString(font, name, offsetX + padding, (offsetY + offsetMaxY - 9) / 2 + 1, 0xFFFFFF);
	}

	public static void optionRight(
		GuiGraphics graphics,
		Font font,
		int minX,
		int maxX,
		int offsetY,
		int padding,
		Component name,
		AbstractWidget toggle
	) {
		int buttonWidth = toggle.getWidth();
		int buttonHeight = toggle.getHeight();

		int offsetX = minX + padding;
		int offsetMaxX = maxX - buttonWidth - 2 * padding;
		int offsetMaxY = offsetY + buttonHeight;

		graphics.fill(offsetX, offsetY, offsetMaxX, offsetMaxY, BG_DARK);
		graphics.drawString(font, name, offsetX + padding, (offsetY + offsetMaxY - 9) / 2 + 1, 0xFFFFFF);

		toggle.setPosition(maxX - buttonWidth - padding, offsetY);
	}

	public static int maxWidth(Font font, Component first, Component... others) {
		int width = font.width(first);
		for (Component component : others) {
			width = Math.max(width, font.width(component));
		}
		return width;
	}

	public static void setVisible(boolean visible, AbstractWidget... widgets) {
		for (AbstractWidget widget : widgets) {
			widget.visible = visible;
		}
	}
}
