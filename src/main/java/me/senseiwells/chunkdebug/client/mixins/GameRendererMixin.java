package me.senseiwells.chunkdebug.client.mixins;

import me.senseiwells.chunkdebug.client.ChunkDebugClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(
		method = "resize",
		at = @At("HEAD")
	)
	private void onResizeDisplay(int width, int height, CallbackInfo ci) {
		ChunkDebugClient.getInstance().onGuiResize(width, height);
	}
}
