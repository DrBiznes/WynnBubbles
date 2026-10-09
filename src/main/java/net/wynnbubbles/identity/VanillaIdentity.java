package net.wynnbubbles.identity;

import java.util.UUID;
import net.minecraft.world.entity.player.Player;

public class VanillaIdentity implements PlayerIdentity {
    @Override
    public UUID canonicalId(Player player) {
        return PlayerIds.canonical(player.getUUID());
    }

    @Override
    public boolean isNpc(Player player) {
        return false;
    }

    @Override
    public boolean isGhost(Player player) {
        return false;
    }
}
