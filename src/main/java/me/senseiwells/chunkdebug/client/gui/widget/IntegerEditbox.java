package me.senseiwells.chunkdebug.client.gui.widget;

import me.senseiwells.chunkdebug.client.utils.RenderUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;

public class IntegerEditbox extends EditBox {
	private final IntConsumer action;
	private int lastValidValue = 0;
	private boolean valid = true;

	public IntegerEditbox(Font font, int width, int height, IntConsumer action) {
		super(font, width, height, Component.empty());
		this.action = action;
		this.setBordered(true);
		this.setIntValue(0);
		this.setResponder(this::onInputChanged);
	}

	public int getIntValue() {
		return this.lastValidValue;
	}

	public void setIntValue(int value) {
		this.setValue(String.valueOf(value));
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ENTER) {
			this.setFocused(false);
			return false;
		}
		return false;
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int minX = this.getX();
		int minY = this.getY();
		int maxX = minX + this.getWidth();
		int maxY = minY + this.getHeight();

		graphics.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), RenderUtils.BG_DARK);
		graphics.fill(minX, minY, maxX, maxY, RenderUtils.BG_LIGHT);

		if (this.isHovered()) {
			graphics.fill(minX, minY, maxX, maxY, 0x10FFFFFF);
		}

		this.height += 1;
		super.renderWidget(graphics, mouseX, mouseY, partialTick);
		this.height -= 1;

		if (!this.valid) {
			graphics.fill(minX, minY, maxX, maxY, 0x22FF0000);
		}
	}

	@Override
	public boolean isBordered() {
		return false;
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!this.isFocused()) {
			this.setIntValue(this.lastValidValue);
			this.action.accept(this.lastValidValue);
		}
	}

	@Override
	public int getInnerWidth() {
		return this.width - 8;
	}

	private void onInputChanged(String string) {
		try {
			this.lastValidValue = Integer.parseInt(string);
			this.valid = true;
		} catch (NumberFormatException ignored) {
			this.valid = false;
		}
	}
}
