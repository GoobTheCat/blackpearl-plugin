package com.playerxi.blackpearl.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A plain wall-clock cooldown: ready by default, starts counting down the
 * moment {@link #consume} is called.
 *
 * <p>An earlier version of this class tried to replicate the original
 * datapack's "only charges while the sword is physically in your inventory"
 * behaviour via a repeating task nudging an accumulated-milliseconds counter
 * forward. That indirection turned out to be fragile in practice (Escape Plan
 * would report "not ready" indefinitely). This version is deliberately
 * boring: one timestamp per player for "when does this become ready again",
 * nothing to tick, nothing that can silently stop advancing.</p>
 */
public final class Cooldown {

    private final long chargeMillis;
    private final Map<UUID, Long> readyAt = new HashMap<>();

    public Cooldown(long chargeMillis) {
        this.chargeMillis = chargeMillis;
    }

    /** True if the player has never used this ability, or their cooldown has elapsed. */
    public boolean isReady(UUID player) {
        Long deadline = readyAt.get(player);
        return deadline == null || System.currentTimeMillis() >= deadline;
    }

    /** Milliseconds remaining until ready, 0 if already ready. */
    public long remainingMillis(UUID player) {
        Long deadline = readyAt.get(player);
        if (deadline == null) {
            return 0L;
        }
        return Math.max(0L, deadline - System.currentTimeMillis());
    }

    /** Starts (or restarts) the cooldown - call this the moment the ability actually fires. */
    public void consume(UUID player) {
        readyAt.put(player, System.currentTimeMillis() + chargeMillis);
    }

    /** Drops all tracking for a player, e.g. on logout, to avoid a slow memory leak. */
    public void forget(UUID player) {
        readyAt.remove(player);
    }
}
