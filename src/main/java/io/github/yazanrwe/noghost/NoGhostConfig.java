package io.github.yazanrwe.noghost;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings stored in {@code config/noghost.json}. Every field has a safe default, so a
 * missing or partial file just falls back to defaults.
 */
public final class NoGhostConfig {

    // ---- ghost hits ----------------------------------------------------------------------

    /** Shorten the vanilla attack lockout that follows a click that hit nothing. ON by default. */
    public boolean improveHits = true;

    /** Ticks attacks stay locked after a missed click (vanilla: 10). */
    public int missCooldownTicks = 0;

    // ---- ghost block healing -------------------------------------------------------------

    /** Re-request the real block state from the server when a predicted block was never confirmed. */
    public boolean autoResync = true;

    /** Extra ticks on top of (ping + 1 server tick) before a prediction is considered unconfirmed. */
    public int resyncExtraTicks = 1;

    /** Lower bound for the wait before resyncing (ticks). */
    public int minResyncDelayTicks = 2;

    /** Upper bound for the wait before resyncing (ticks). */
    public int maxResyncDelayTicks = 20;

    /** Safety cap on resync packets sent per client tick. */
    public int maxResyncsPerTick = 6;

    /** When the server teleports you back (rubberband), resync every unconfirmed block immediately. */
    public boolean resyncOnRubberband = true;

    // ---- delay reduction -----------------------------------------------------------------

    /**
     * Master switch for the two cooldown settings below. OFF by default: lowering vanilla
     * cooldowns behaves like "fast place / fast break" and many servers' anti-cheat flags it.
     */
    public boolean reduceCooldowns = false;

    /** Ticks to wait between breaking blocks while holding attack (vanilla: 5). */
    public int breakCooldownTicks = 0;

    /** Ticks to wait between placing/using while holding use (vanilla: 4). */
    public int placeCooldownTicks = 0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static NoGhostConfig loadOrCreate(Path file) {
        NoGhostConfig cfg = new NoGhostConfig();
        boolean mayWrite = true;

        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                NoGhostConfig read = GSON.fromJson(reader, NoGhostConfig.class);
                if (read != null) {
                    cfg = read;
                }
            } catch (IOException | JsonParseException e) {
                // Never overwrite a file the user may be in the middle of editing.
                mayWrite = false;
                NoGhost.LOGGER.warn("Could not read {} ({}); using defaults for this session.", file, e.getMessage());
            }
        }

        cfg.sanitize();

        if (mayWrite) {
            try {
                Files.writeString(file, GSON.toJson(cfg));
            } catch (IOException e) {
                NoGhost.LOGGER.warn("Could not write {}: {}", file, e.getMessage());
            }
        }
        return cfg;
    }

    private void sanitize() {
        resyncExtraTicks = clamp(resyncExtraTicks, 0, 20);
        minResyncDelayTicks = clamp(minResyncDelayTicks, 0, 40);
        maxResyncDelayTicks = clamp(maxResyncDelayTicks, minResyncDelayTicks, 200);
        maxResyncsPerTick = clamp(maxResyncsPerTick, 1, 64);
        missCooldownTicks = clamp(missCooldownTicks, 0, 10);
        breakCooldownTicks = clamp(breakCooldownTicks, 0, 10);
        placeCooldownTicks = clamp(placeCooldownTicks, 0, 10);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
