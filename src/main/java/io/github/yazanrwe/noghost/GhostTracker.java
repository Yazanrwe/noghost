package io.github.yazanrwe.noghost;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks block changes the client predicted (broke / placed) and makes sure the server
 * actually answered for them.
 *
 * <p>A normal server always sends a block update for a position the player acted on, whether
 * the action succeeded or was rejected. If no update shows up within roughly one round trip,
 * the server swallowed the action (plugin, anti-cheat, version translation, packet loss...) and
 * the client may be left showing a ghost block. In that case we ask the server for the real
 * state of that single position.
 *
 * <p>The request is a {@code ABORT_DESTROY_BLOCK} action on the position, which servers answer
 * by re-sending the block's current state. It is only sent while the player is not mining, so it
 * never interferes with a block that is being broken.
 *
 * <p>All methods run on the client thread.
 */
public final class GhostTracker {
    private static final int MAX_TRACKED = 128;

    /** packed BlockPos -> client tick at which the prediction is considered unconfirmed. */
    private static final Map<Long, Integer> PENDING = new LinkedHashMap<>();
    private static int ticks;

    private GhostTracker() {
    }

    /** The client just changed {@code pos} on its own (prediction). */
    public static void onPredicted(BlockPos pos) {
        NoGhostConfig cfg = NoGhost.config();
        if (!cfg.autoResync) {
            return;
        }

        int delay = latencyTicks() + 1 + cfg.resyncExtraTicks;
        delay = Math.max(cfg.minResyncDelayTicks, Math.min(cfg.maxResyncDelayTicks, delay));
        PENDING.put(pos.asLong(), ticks + delay);

        if (PENDING.size() > MAX_TRACKED) {
            Iterator<Long> oldest = PENDING.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
    }

    /** The server told us something about {@code pos}, so it is not silently ignoring us. */
    public static void onServerUpdate(BlockPos pos) {
        if (!PENDING.isEmpty()) {
            PENDING.remove(pos.asLong());
        }
    }

    /** The server moved us back: collision with a ghost block is a likely cause, check everything now. */
    public static void onRubberband() {
        if (!NoGhost.config().resyncOnRubberband || PENDING.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, Integer> entry : PENDING.entrySet()) {
            entry.setValue(ticks);
        }
    }

    public static void tick(MinecraftClient client) {
        ticks++;
        if (PENDING.isEmpty()) {
            return;
        }

        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (client.world == null || client.player == null || handler == null) {
            PENDING.clear();
            return;
        }

        // Do not poke the server while a block is being mined.
        if (client.interactionManager != null && client.interactionManager.isBreakingBlock()) {
            return;
        }

        int budget = NoGhost.config().maxResyncsPerTick;
        Iterator<Map.Entry<Long, Integer>> it = PENDING.entrySet().iterator();
        while (it.hasNext() && budget > 0) {
            Map.Entry<Long, Integer> entry = it.next();
            if (entry.getValue() > ticks) {
                continue;
            }
            it.remove();
            BlockPos pos = BlockPos.fromLong(entry.getKey());
            handler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.DOWN));
            budget--;
        }
    }

    /** Round-trip time to the server in ticks (rounded up). */
    private static int latencyTicks() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null || client.player == null) {
            return 2;
        }
        PlayerListEntry entry = handler.getPlayerListEntry(client.player.getUuid());
        int latencyMs = entry == null ? 100 : entry.getLatency();
        return (Math.max(0, latencyMs) + 49) / 50;
    }
}
