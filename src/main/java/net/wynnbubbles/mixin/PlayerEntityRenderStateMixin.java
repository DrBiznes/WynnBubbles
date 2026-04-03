package net.wynnbubbles.mixin;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.wynnbubbles.accessor.PlayerEntityRenderStateAccessor;
import net.wynnbubbles.util.BubbleMessage;
import net.wynnbubbles.util.HistoricalData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class PlayerEntityRenderStateMixin implements PlayerEntityRenderStateAccessor {

    @Unique
    private HistoricalData<BubbleMessage> wynnbubbles$bubbleMessages;

    @Override
    public HistoricalData<BubbleMessage> wynnbubbles$getBubbleMessages() {
        return wynnbubbles$bubbleMessages;
    }

    @Override
    public void wynnbubbles$setBubbleMessages(HistoricalData<BubbleMessage> messages) {
        this.wynnbubbles$bubbleMessages = messages;
    }
}
