package net.wynnbubbles.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.wynnbubbles.WynnBubbles;
import net.wynnbubbles.accessor.BubbleRenderStateAccessor;
import net.wynnbubbles.bubble.Bubble;
import net.wynnbubbles.client.BubbleRenderer;
import net.wynnbubbles.client.NametagStackTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Unique
    private static final String SUBMIT =
            "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V";

    /** Gap between the top of the nametag stack and the tip of the bubble arrow, in blocks. */
    @Unique
    private static final float GAP_ABOVE_NAMETAGS = 0.06f;

    /** Height above the head when nothing at all is drawn there (e.g. your own player). */
    @Unique
    private static final float GAP_ABOVE_BARE_HEAD = 0.3f;

    @Inject(method = SUBMIT, at = @At("HEAD"))
    private void wynnbubbles$beginTracking(
            LivingEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo ci) {
        if (wynnbubbles$bubbles(state).isEmpty()) return;
        NametagStackTracker.begin(poseStack, state.boundingBoxHeight);
    }

    // TAIL is after super.submit, which is where the nametag (and everything mods hang off it) is submitted
    @Inject(method = SUBMIT, at = @At("TAIL"))
    private void wynnbubbles$submitBubbles(
            LivingEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo ci) {
        List<Bubble> bubbles = wynnbubbles$bubbles(state);
        if (bubbles.isEmpty()) return;

        float stackTop = NametagStackTracker.end();
        float anchor = Float.isNaN(stackTop)
                ? state.boundingBoxHeight + GAP_ABOVE_BARE_HEAD
                : stackTop + GAP_ABOVE_NAMETAGS;

        BubbleRenderer.submit(
                poseStack,
                collector,
                camera,
                Minecraft.getInstance().font,
                bubbles,
                anchor + WynnBubbles.CONFIG.chatHeight);
    }

    @Unique
    private static List<Bubble> wynnbubbles$bubbles(LivingEntityRenderState state) {
        if (!(state instanceof AvatarRenderState) || state.isInvisible) return List.of();
        return ((BubbleRenderStateAccessor) state).wynnbubbles$getBubbles();
    }
}
