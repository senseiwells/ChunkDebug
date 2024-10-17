package me.senseiwells.chunkdebug.client.gui.widget;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import me.senseiwells.chunkdebug.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import static me.senseiwells.chunkdebug.client.utils.RenderUtils.HL;

public class ToggleButton extends AbstractButton {
	private final BooleanConsumer action;
	private boolean toggled;

	public ToggleButton(int x, int y, int size, BooleanConsumer action) {
		super(x, y, size, size, Component.empty());
		this.action = action;
	}

	public ToggleButton(int x, int y, int size) {
		this(x, y, size, b -> {});
	}

	public void setToggled(boolean toggled) {
		this.toggled = toggled;
	}

	public boolean isToggled() {
		return this.toggled;
	}

	@Override
	public void onPress() {
		this.toggled = !this.toggled;
		this.action.accept(this.toggled);
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int minX = this.getX();
		int minY = this.getY();
		int maxX = minX + this.getWidth();
		int maxY = minY + this.getHeight();

		graphics.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), RenderUtils.BG_DARK);
		graphics.fill(minX, minY, maxX, maxY, RenderUtils.BG_LIGHT);
		if (this.toggled) {
			int dx = this.getWidth() / 4;
			int dy = this.getHeight() / 4;
			graphics.fill(minX + dx, minY + dy, maxX - dx, maxY - dy, HL);
		}

		if (this.isHovered()) {
			graphics.fill(minX, minY, maxX, maxY, 0x10FFFFFF);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

	}
}
