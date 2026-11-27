package io.github.yazanrwe.noghost.mixin;

import io.github.yazanrwe.noghost.GhostTracker;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {

    /** Every server-sent block change (single or batched) goes through here. */
    @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
    private void noghost$serverBlockUpdate(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        GhostTracker.onServerUpdate(pos);
    }
}
