package me.senseiwells.chunkdebug.client.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import me.senseiwells.chunkdebug.client.ChunkDebugClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.LayeredDraw;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
	@Inject(
		method = "<init>",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/LayeredDraw;add(Lnet/minecraft/client/gui/LayeredDraw;Ljava/util/function/BooleanSupplier;)Lnet/minecraft/client/gui/LayeredDraw;",
			ordinal = 0
		)
	)
	private void addChunkDebugMinimapLayer(Minecraft minecraft, CallbackInfo ci, @Local(ordinal = 0) LayeredDraw layered) {
		layered.add(ChunkDebugClient.getInstance()::onGuiRender);
	}
}
