package net.wynnbubbles.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.wynnbubbles.WynnBubbles;

/**
 * Soft-dependency helper for Wynntils.
 *
 * Wynntils stacks nametags above players (base nametag + marker + account type +
 * any WynnTitles entries) at ~0.259 blocks per row. Rather than hooking into
 * Wynntils' event system across mod loaders, WynnBubbles uses a configurable
 * static offset that the user can tune via ModMenu.
 *
 * Typical values:
 *   0.0  — Wynntils not installed / plain nametag only
 *   0.5  — base nametag + Wynntils marker (default)
 *   0.7  — + account type row
 *   1.0+ — + WynnTitles or extra custom nametag rows
 */
public final class WynntilsCompat {

    private static boolean wynntilsLoaded = false;

    public static void init() {
        wynntilsLoaded = FabricLoader.getInstance().isModLoaded("wynntils");
    }

    /**
     * Returns the extra Y offset (in world units) to push bubbles above the
     * Wynntils nametag stack. Returns 0 when Wynntils is not installed.
     */
    public static float getOffset() {
        return wynntilsLoaded ? WynnBubbles.CONFIG.wynntilsOffset : 0f;
    }

    public static boolean isLoaded() {
        return wynntilsLoaded;
    }

    private WynntilsCompat() {}
}
