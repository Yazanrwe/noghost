package io.github.yazanrwe.noghost.mixin;

import io.github.yazanrwe.noghost.GhostTracker;
import io.github.yazanrwe.noghost.NoGhost;
import io.github.yazanrwe.noghost.NoGhostConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.BlockItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Shadow
    private int blockBreakingCooldown;

    @Unique
    private boolean noghost$holdingBlockItem;

    /** The client removed a block on its own: remember it until the server confirms. */
    @Inject(method = "breakBlock", at = @At("RETURN"))
    private void noghost$afterBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            GhostTracker.onPredicted(pos);
        }
    }

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void noghost$beforeInteract(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                        CallbackInfoReturnable<ActionResult> cir) {
        // Remember this now: the stack may be empty by the time interactBlock returns.
        this.noghost$holdingBlockItem = player.getStackInHand(hand).getItem() instanceof BlockItem;
    }

    /** The client placed a block on its own: remember it until the server confirms. */
    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void noghost$afterInteract(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                       CallbackInfoReturnable<ActionResult> cir) {
        ActionResult result = cir.getReturnValue();
        if (this.noghost$holdingBlockItem && result != null && result.isAccepted()) {
            BlockPos clicked = hitResult.getBlockPos();
            GhostTracker.onPredicted(clicked.offset(hitResult.getSide()));
            // Placing onto a replaceable block (grass, snow...) changes the clicked block itself.
            GhostTracker.onPredicted(clicked);
        }
    }

    /** Shorten the vanilla pause between two broken blocks (only when enabled in the config). */
    @Inject(method = {"attackBlock", "updateBlockBreakingProgress"}, at = @At("RETURN"))
    private void noghost$trimBreakCooldown(CallbackInfoReturnable<Boolean> cir) {
        NoGhostConfig cfg = NoGhost.config();
        if (cfg.reduceCooldowns && this.blockBreakingCooldown > cfg.breakCooldownTicks) {
            this.blockBreakingCooldown = cfg.breakCooldownTicks;
        }
    }
}
