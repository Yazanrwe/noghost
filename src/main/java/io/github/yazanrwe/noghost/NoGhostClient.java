package io.github.yazanrwe.noghost;

import net.fabricmc.api.ClientModInitializer;

public final class NoGhostClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NoGhost.init();
    }
}
