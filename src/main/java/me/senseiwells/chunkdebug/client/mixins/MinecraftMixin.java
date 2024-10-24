package me.senseiwells.chunkdebug.client.mixins;

import com.mojang.blaze3d.platform.Window;
import me.senseiwells.chunkdebug.client.ChunkDebugClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Shadow @Final private Window window;

	@Inject(
		method = "resizeDisplay",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
		)
	)
	private void onResizeDisplay(CallbackInfo ci) {
		ChunkDebugClient.getInstance().onGuiResize(
			this.window.getGuiScaledWidth(),
			this.window.getGuiScaledHeight()
		);
	}
}
