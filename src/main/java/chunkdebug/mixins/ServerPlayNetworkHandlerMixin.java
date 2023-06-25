package chunkdebug.mixins;

import chunkdebug.ChunkDebugServer;
import chunkdebug.feature.ChunkServerNetworkHandler;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements ServerPlayPacketListener {
	@Shadow public ServerPlayerEntity player;

	@Inject(method = "onCustomPayload", at = @At("HEAD"), cancellable = true)
	private void onCustomPayload(CustomPayloadC2SPacket packet, CallbackInfo ci) {
		CustomPayloadC2SPacketAccessor iPacket = (CustomPayloadC2SPacketAccessor) packet;
		if (ChunkServerNetworkHandler.ESSENTIAL_CHANNEL.equals(iPacket.getChannel())) {
			NetworkThreadUtils.forceMainThread(packet, this, (ServerWorld) this.player.getWorld());
			ChunkDebugServer.chunkNetHandler.handlePacket(iPacket.getData(), this.player);
			ci.cancel();
		}
	}

	@Inject(method = "onDisconnected", at = @At("HEAD"))
	private void onDisconnect(Text reason, CallbackInfo ci) {
		ChunkDebugServer.chunkNetHandler.removePlayer(this.player);
	}
}
