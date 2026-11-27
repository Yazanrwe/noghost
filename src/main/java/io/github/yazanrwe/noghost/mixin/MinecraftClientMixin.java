package io.github.yazanrwe.noghost.mixin;

import io.github.yazanrwe.noghost.GhostTracker;
import io.github.yazanrwe.noghost.NoGhost;
import io.github.yazanrwe.noghost.NoGhostConfig;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow
    private int itemUseCooldown;

    @Inject(method = "tick", at = @At("HEAD"))
    private void noghost$tick(CallbackInfo ci) {
        GhostTracker.tick((MinecraftClient) (Object) this);
    }

    /** Shorten the vanilla pause between two placements/uses (only when enabled in the config). */
    @Inject(method = "doItemUse", at = @At("RETURN"))
    private void noghost$trimUseCooldown(CallbackInfo ci) {
        NoGhostConfig cfg = NoGhost.config();
        if (cfg.reduceCooldowns && this.itemUseCooldown > cfg.placeCooldownTicks) {
            this.itemUseCooldown = cfg.placeCooldownTicks;
        }
    }
}
