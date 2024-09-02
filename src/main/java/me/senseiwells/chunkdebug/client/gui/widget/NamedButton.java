package me.senseiwells.chunkdebug.client.gui.widget;

import me.senseiwells.chunkdebug.client.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class NamedButton extends AbstractButton {
	private final Runnable action;

	public NamedButton(int x, int y, int width, int height, Component message, Runnable action) {
		super(x, y, width, height, message);
		this.action = action;
	}

	@Override
	public void onPress() {
		this.action.run();
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		int minX = this.getX();
		int minY = this.getY();
		int maxX = minX + this.getWidth();
		int maxY = minY + this.getHeight();

		graphics.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), RenderUtils.BG_DARK);
		graphics.fill(minX, minY, maxX, maxY, RenderUtils.BG_LIGHT);

		renderScrollingString(graphics, minecraft.font, this.getMessage(), minX, minY, maxX, maxY);

		if (this.isHovered()) {
			graphics.fill(minX, minY, maxX, maxY, 0x10FFFFFF);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

	}

	public static void renderScrollingString(
		GuiGraphics guiGraphics,
		Font font,
		Component text,
		int minX,
		int minY,
		int maxX,
		int maxY
	) {
		renderScrollingString(guiGraphics, font, text, (minX + maxX) / 2, minX, minY, maxX, maxY, 0xFFFFFFFF);
	}
}
