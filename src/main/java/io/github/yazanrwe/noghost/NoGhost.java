package io.github.yazanrwe.noghost;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NoGhost {
    public static final String MOD_ID = "noghost";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static NoGhostConfig config = new NoGhostConfig();

    private NoGhost() {
    }

    public static NoGhostConfig config() {
        return config;
    }

    static void init() {
        config = NoGhostConfig.loadOrCreate(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));
        LOGGER.info("NoGhost loaded: autoResync={}, resyncOnRubberband={}, reduceCooldowns={} (break={}, place={})",
                config.autoResync, config.resyncOnRubberband, config.reduceCooldowns,
                config.breakCooldownTicks, config.placeCooldownTicks);
    }
}
