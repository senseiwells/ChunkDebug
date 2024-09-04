package me.senseiwells.chunkdebug.client.mixins;

import me.senseiwells.chunkdebug.client.ChunkDebugClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
	// We do this instead of adding a new layer for shader compatability reasons
	@Inject(
		method = "renderEffects",
		at = @At("TAIL")
	)
	private void renderChunkDebugMinimap(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
		ChunkDebugClient.getInstance().onGuiRender(graphics, tracker);
	}
}
