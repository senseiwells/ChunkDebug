package me.senseiwells.chunkdebug.server.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import me.senseiwells.chunkdebug.server.ChunkDebugServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(
		method = "op(Lnet/minecraft/server/players/NameAndId;Ljava/util/Optional;Ljava/util/Optional;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/players/PlayerList;sendPlayerPermissionLevel(Lnet/minecraft/server/level/ServerPlayer;)V"
		)
	)
	private void onOpPlayer(CallbackInfo ci, @Local ServerPlayer player) {
		ChunkDebugServer.getInstance().onOpPlayer(player);
	}

	@Inject(
		method = "deop",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/players/PlayerList;sendPlayerPermissionLevel(Lnet/minecraft/server/level/ServerPlayer;)V"
		)
	)
	private void onDeOpPlayer(CallbackInfo ci, @Local ServerPlayer player) {
		ChunkDebugServer.getInstance().onDeOpPlayer(player);
	}
}
