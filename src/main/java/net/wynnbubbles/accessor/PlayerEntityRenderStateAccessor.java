package net.wynnbubbles.accessor;

import net.wynnbubbles.util.BubbleMessage;
import net.wynnbubbles.util.HistoricalData;

public interface PlayerEntityRenderStateAccessor {
    HistoricalData<BubbleMessage> wynnbubbles$getBubbleMessages();
    void wynnbubbles$setBubbleMessages(HistoricalData<BubbleMessage> messages);
}
