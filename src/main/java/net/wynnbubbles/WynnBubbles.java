package net.wynnbubbles;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.wynnbubbles.compat.WynntilsCompat;
import net.wynnbubbles.config.WynnBubblesConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WynnBubbles implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("wynnbubbles");
    public static WynnBubblesConfig CONFIG = new WynnBubblesConfig();

    @Override
    public void onInitializeClient() {
        AutoConfig.register(WynnBubblesConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(WynnBubblesConfig.class).getConfig();
        WynntilsCompat.init();
    }
}