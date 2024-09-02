package me.senseiwells.chunkdebug.client.gui.widget;

import me.senseiwells.chunkdebug.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import static me.senseiwells.chunkdebug.client.utils.RenderUtils.HL;

public class ArrowButton extends AbstractButton {
	private final Direction direction;
	private final Runnable action;

	public ArrowButton(Direction direction, int x, int y, int size, Runnable pressed) {
		super(x, y, size, size, Component.empty());
		this.direction = direction;
		this.action = pressed;
	}

	@Override
	public void onPress() {
		this.action.run();
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int minX = this.getX();
		int minY = this.getY();
		int maxX = minX + this.getWidth();
		int maxY = minY + this.getHeight();

		graphics.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), RenderUtils.BG_DARK);
		graphics.fill(minX, minY, maxX, maxY, RenderUtils.BG_LIGHT);
		int dx = this.getWidth() / 4;
		int dy = this.getHeight() / 4;
		int angle = this.direction.ordinal() * 360 / 4;
		RenderUtils.triangle(graphics, minX + dx, minY + dy, maxX - dx, maxY - dy, angle, HL);

		if (this.isHovered()) {
			graphics.fill(minX, minY, maxX, maxY, 0x10FFFFFF);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

	}

	public enum Direction {
		RIGHT, DOWN, LEFT, UP
	}
}
