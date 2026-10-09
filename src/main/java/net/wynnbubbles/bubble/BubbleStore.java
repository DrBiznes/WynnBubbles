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

    public void add(UUID playerId, Bubble bubble, int maxBubbles) {
        Deque<Bubble> deque = bubbles.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        deque.addFirst(bubble);
        while (deque.size() > Math.max(1, maxBubbles)) deque.removeLast();
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
