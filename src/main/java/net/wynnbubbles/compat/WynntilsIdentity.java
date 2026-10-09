package net.wynnbubbles.compat;

import com.wynntils.core.components.Models;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.wynnbubbles.identity.PlayerIdentity;

/** Only loaded when Wynntils is present; nothing else may reference Wynntils classes. */
public class WynntilsIdentity implements PlayerIdentity {
    @Override
    public UUID canonicalId(Player player) {
        return Models.Player.getUserUUID(player);
    }

    @Override
    public boolean isNpc(Player player) {
        return Models.Player.isNpc(player);
    }

    @Override
    public boolean isGhost(Player player) {
        return Models.Player.isPlayerGhost(player);
    }
}
