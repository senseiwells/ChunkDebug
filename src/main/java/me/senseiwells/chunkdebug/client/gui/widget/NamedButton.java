package me.senseiwells.chunkdebug.client.gui.widget;

import me.senseiwells.chunkdebug.client.utils.RenderUtils;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public class NamedButton extends AbstractButton {
	private final Runnable action;

	public NamedButton(int x, int y, int width, int height, Component message, Runnable action) {
		super(x, y, width, height, message);
		this.action = action;
	}

	@Override
	public void onPress(InputWithModifiers modifiers) {
		this.action.run();
	}

	@Override
	protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int minX = this.getX();
		int minY = this.getY();
		int maxX = minX + this.getWidth();
		int maxY = minY + this.getHeight();

		graphics.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), RenderUtils.BG_DARK);
		graphics.fill(minX, minY, maxX, maxY, RenderUtils.BG_LIGHT);

		ActiveTextCollector collector = graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE);
		this.renderDefaultLabel(collector);

		if (this.isHovered()) {
			graphics.fill(minX, minY, maxX, maxY, 0x10FFFFFF);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

	}
}
