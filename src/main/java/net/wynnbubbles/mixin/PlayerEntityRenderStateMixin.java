package net.wynnbubbles.mixin;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.wynnbubbles.accessor.PlayerEntityRenderStateAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(PlayerEntityRenderState.class)
public class PlayerEntityRenderStateMixin implements PlayerEntityRenderStateAccessor {

    @Unique
    private List<String> bubbleText;

    @Override
    public List<String> getBubbleText() {
        return bubbleText;
    }

    @Override
    public void setBubbleText(List<String> text) {
        this.bubbleText = text;
    }
}
