package net.wynnbubbles.bubble;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bubbles per account, newest first. Keyed by canonical account id rather than stored on the
 * entity, so they survive a player swapping between a real entity and a ghost.
 */
public class BubbleStore {
    private final Map<UUID, Deque<Bubble>> bubbles = new HashMap<>();

    /**
     * Chat mods can push one incoming line into the chat HUD several times (Wynntils does it once
     * per chat tab), so a repeat of the newest bubble within a tick of it is the same message.
     */
    private static final int DUPLICATE_WINDOW_TICKS = 1;

    /** @return false if the bubble was dropped as a repeat of the newest one */
    public boolean add(UUID playerId, Bubble bubble, int maxBubbles) {
        Deque<Bubble> deque = bubbles.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        Bubble newest = deque.peekFirst();
        if (newest != null
                && bubble.createdTick() - newest.createdTick() <= DUPLICATE_WINDOW_TICKS
                && newest.type() == bubble.type()
                && newest.text().getString().equals(bubble.text().getString())) {
            return false;
        }
        deque.addFirst(bubble);
        while (deque.size() > Math.max(1, maxBubbles)) deque.removeLast();
        return true;
    }

    public List<Bubble> get(UUID playerId) {
        Deque<Bubble> deque = bubbles.get(playerId);
        return deque == null ? List.of() : List.copyOf(deque);
    }

    public void expire(long currentTick, int lifetimeTicks) {
        bubbles.values().removeIf(deque -> {
            deque.removeIf(bubble -> currentTick - bubble.createdTick() > lifetimeTicks);
            return deque.isEmpty();
        });
    }

    public void clear() {
        bubbles.clear();
    }
}
