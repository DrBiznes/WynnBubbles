package net.wynnbubbles.accessor;

import net.wynnbubbles.util.BubbleMessage;
import net.wynnbubbles.util.HistoricalData;

public interface AbstractClientPlayerEntityAccessor {
    HistoricalData<BubbleMessage> wynnbubbles$getBubbleMessages();
    void wynnbubbles$addBubbleMessage(BubbleMessage message);
}
