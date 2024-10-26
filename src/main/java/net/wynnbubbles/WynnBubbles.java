package net.wynnbubbles;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.wynnbubbles.config.WynnBubblesConfig;

public class WynnBubbles implements ClientModInitializer {

    public static WynnBubblesConfig CONFIG = new WynnBubblesConfig();

    @Override
    public void onInitializeClient() {
        AutoConfig.register(WynnBubblesConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(WynnBubblesConfig.class).getConfig();
    }
}