package net.wynnbubbles.identity;

import java.util.UUID;

public final class PlayerIds {

    /**
     * Wynncraft shows players from other servers as "ghosts": the same account UUID with the
     * version nibble rewritten from 4 to 2. Folding every UUID back to version 4 gives one key
     * for a player whether they are currently a real entity or a ghost. Same conversion as
     * Wynntils' PlayerModel#getUserUUID, but limited to version 2 so offline-mode (version 3)
     * UUIDs are left alone.
     */
    public static UUID canonical(UUID uuid) {
        if (uuid == null || uuid.version() != 2) return uuid;
        return new UUID((uuid.getMostSignificantBits() & ~0xF000L) | 0x4000L, uuid.getLeastSignificantBits());
    }

    private PlayerIds() {}
}
