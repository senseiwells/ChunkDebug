package chunkdebug.mixins;

import chunkdebug.ChunkDebugServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@Shadow
	public abstract Iterable<ServerWorld> getWorlds();

	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		ChunkDebugServer.chunkNetHandler.tickUpdate();
	}

	@Inject(method = "loadWorld", at = @At("HEAD"))
	private void onLoadWorldPre(CallbackInfo ci) {
		ChunkDebugServer.server = (MinecraftServer) (Object) this;
	}

	@Inject(method = "loadWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;createWorlds(Lnet/minecraft/server/WorldGenerationProgressListener;)V", shift = At.Shift.AFTER))
	private void onLoadWorldPost(CallbackInfo ci) {
		this.getWorlds().forEach(ChunkDebugServer.chunkNetHandler::addWorld);
	}
}
