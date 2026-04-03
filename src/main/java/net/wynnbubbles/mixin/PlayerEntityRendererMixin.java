package net.wynnbubbles.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.wynnbubbles.WynnBubbles;
import net.wynnbubbles.accessor.AbstractClientPlayerEntityAccessor;
import net.wynnbubbles.accessor.PlayerEntityRenderStateAccessor;
import net.wynnbubbles.util.BubbleMessage;
import net.wynnbubbles.util.HistoricalData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(AvatarRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, AvatarRenderState, PlayerModel> {

    public PlayerEntityRendererMixin(EntityRendererProvider.Context ctx, PlayerModel model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL")
    )
    private void wynnbubbles$updateRenderState(Avatar player, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        if (!(player instanceof AbstractClientPlayer abstractPlayer)) return;
        if (abstractPlayer.isInvisible() || !abstractPlayer.isAlive()) {
            ((PlayerEntityRenderStateAccessor) state).wynnbubbles$setBubbleMessages(null);
            return;
        }

        AbstractClientPlayerEntityAccessor accessor = (AbstractClientPlayerEntityAccessor) abstractPlayer;
        HistoricalData<BubbleMessage> messages = accessor.wynnbubbles$getBubbleMessages();

        // Evict messages that have exceeded the configured display time
        messages.removeIf(msg -> abstractPlayer.tickCount - msg.createdAtTick() > WynnBubbles.CONFIG.chatTime);

        if (messages.isEmpty()) {
            ((PlayerEntityRenderStateAccessor) state).wynnbubbles$setBubbleMessages(null);
        } else {
            ((PlayerEntityRenderStateAccessor) state).wynnbubbles$setBubbleMessages(messages);
        }
    }
}
