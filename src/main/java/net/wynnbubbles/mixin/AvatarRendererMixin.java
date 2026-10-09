package net.wynnbubbles.mixin;

import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.wynnbubbles.WynnBubbles;
import net.wynnbubbles.accessor.BubbleRenderStateAccessor;
import net.wynnbubbles.identity.Identities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {

    @Inject(
            method =
                    "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void wynnbubbles$extractBubbles(
            Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        BubbleRenderStateAccessor accessor = (BubbleRenderStateAccessor) state;
        if (avatar instanceof AbstractClientPlayer player && !player.isInvisible() && player.isAlive()) {
            accessor.wynnbubbles$setBubbles(WynnBubbles.BUBBLES.get(Identities.canonicalId(player)));
        } else {
            accessor.wynnbubbles$setBubbles(List.of());
        }
    }
}
