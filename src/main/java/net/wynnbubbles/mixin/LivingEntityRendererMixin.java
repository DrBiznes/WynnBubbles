package net.wynnbubbles.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.wynnbubbles.accessor.PlayerEntityRenderStateAccessor;
import net.wynnbubbles.client.BubbleRenderer;
import net.wynnbubbles.util.BubbleMessage;
import net.wynnbubbles.util.HistoricalData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
        at = @At("HEAD")
    )
    private void wynnbubbles$renderBubbles(
            LivingEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        if (!(state instanceof AvatarRenderState avatarState)) return;
        if (avatarState.isInvisible) return;

        HistoricalData<BubbleMessage> messages = ((PlayerEntityRenderStateAccessor) avatarState).wynnbubbles$getBubbleMessages();
        if (messages == null || messages.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        if (net.wynnbubbles.WynnBubbles.CONFIG.debugMode) {
            net.wynnbubbles.WynnBubbles.LOGGER.info("[WynnBubbles] renderBubbles: {} message(s), height={}", messages.size(), avatarState.boundingBoxHeight);
        }

        BubbleRenderer.renderBubbles(
                poseStack,
                cameraState.orientation,
                client.font,
                messages,
                avatarState.boundingBoxHeight + 0.3f
        );
    }
}
