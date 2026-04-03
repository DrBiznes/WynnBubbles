package net.wynnbubbles.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import net.wynnbubbles.accessor.AbstractClientPlayerEntityAccessor;
import net.wynnbubbles.util.BubbleMessage;
import net.wynnbubbles.util.HistoricalData;

@Environment(EnvType.CLIENT)
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerEntityMixin implements AbstractClientPlayerEntityAccessor {

    @Unique
    private final HistoricalData<BubbleMessage> wynnbubbles$bubbleMessages = new HistoricalData<>(5);

    @Override
    public HistoricalData<BubbleMessage> wynnbubbles$getBubbleMessages() {
        return wynnbubbles$bubbleMessages;
    }

    @Override
    public void wynnbubbles$addBubbleMessage(BubbleMessage message) {
        wynnbubbles$bubbleMessages.add(message);
    }
}
