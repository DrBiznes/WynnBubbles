package net.wynnbubbles;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.wynnbubbles.bubble.BubbleStore;
import net.wynnbubbles.config.WynnBubblesConfig;
import net.wynnbubbles.identity.Identities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WynnBubbles implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("wynnbubbles");
    public static final BubbleStore BUBBLES = new BubbleStore();
    public static WynnBubblesConfig CONFIG = new WynnBubblesConfig();

    private static long clientTick;

    public static long clientTick() {
        return clientTick;
    }

    @Override
    public void onInitializeClient() {
        AutoConfig.register(WynnBubblesConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(WynnBubblesConfig.class).getConfig();
        Identities.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            clientTick++;
            BUBBLES.expire(clientTick, CONFIG.chatTime);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BUBBLES.clear());
    }
}
