package net.wynnbubbles.mixin;

import java.util.List;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.wynnbubbles.accessor.BubbleRenderStateAccessor;
import net.wynnbubbles.bubble.Bubble;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements BubbleRenderStateAccessor {

    @Unique
    private List<Bubble> wynnbubbles$bubbles = List.of();

    @Override
    public List<Bubble> wynnbubbles$getBubbles() {
        return wynnbubbles$bubbles;
    }

    @Override
    public void wynnbubbles$setBubbles(List<Bubble> bubbles) {
        this.wynnbubbles$bubbles = bubbles;
    }
}
