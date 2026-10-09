package net.wynnbubbles.identity;

import java.util.UUID;
import net.minecraft.world.entity.player.Player;

/** How a player entity maps to an account. Wynntils answers when installed, vanilla otherwise. */
public interface PlayerIdentity {
    UUID canonicalId(Player player);

    boolean isNpc(Player player);

    boolean isGhost(Player player);
}
