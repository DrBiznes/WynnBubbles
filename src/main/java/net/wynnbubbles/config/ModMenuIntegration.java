package net.wynnbubbles.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Without Cloth Config there is no screen; Mod Menu then hides the Configure button
        // and the config file is edited by hand instead.
        if (!FabricLoader.getInstance().isModLoaded("cloth-config2")) {
            return parent -> null;
        }
        return ClothConfigScreen::create;
    }
}
