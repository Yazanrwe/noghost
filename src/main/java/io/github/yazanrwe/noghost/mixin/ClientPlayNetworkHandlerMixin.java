package io.github.yazanrwe.noghost.mixin;

import io.github.yazanrwe.noghost.GhostTracker;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    /** Server teleported us (rubberband): resync unconfirmed blocks right away. */
    @Inject(method = "onPlayerPositionLook", at = @At("TAIL"))
    private void noghost$onRubberband(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        GhostTracker.onRubberband();
    }
}
