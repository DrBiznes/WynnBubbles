package net.wynnbubbles.identity;

import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.wynnbubbles.WynnBubbles;

/** Wynntils first, vanilla as the fallback when it is missing or throws. */
public final class Identities {
    private static final PlayerIdentity VANILLA = new VanillaIdentity();
    private static PlayerIdentity wynntils;

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("wynntils")) return;
        try {
            wynntils = (PlayerIdentity) Class.forName("net.wynnbubbles.compat.WynntilsIdentity")
                    .getDeclaredConstructor()
                    .newInstance();
            WynnBubbles.LOGGER.info("Using Wynntils for player identity");
        } catch (ReflectiveOperationException | LinkageError e) {
            WynnBubbles.LOGGER.warn("Wynntils is installed but its player model is unavailable", e);
        }
    }

    public static boolean usingWynntils() {
        return wynntils != null;
    }

    public static UUID canonicalId(Player player) {
        if (wynntils != null) {
            try {
                UUID id = wynntils.canonicalId(player);
                if (id != null) return id;
            } catch (RuntimeException | LinkageError e) {
                disable(e);
            }
        }
        return VANILLA.canonicalId(player);
    }

    public static boolean isNpc(Player player) {
        if (wynntils != null) {
            try {
                return wynntils.isNpc(player);
            } catch (RuntimeException | LinkageError e) {
                disable(e);
            }
        }
        return VANILLA.isNpc(player);
    }

    public static boolean isGhost(Player player) {
        if (wynntils != null) {
            try {
                return wynntils.isGhost(player);
            } catch (RuntimeException | LinkageError e) {
                disable(e);
            }
        }
        return VANILLA.isGhost(player);
    }

    private static void disable(Throwable e) {
        WynnBubbles.LOGGER.warn("Wynntils player lookup failed, falling back to vanilla", e);
        wynntils = null;
    }

    private Identities() {}
}
