package net.wynnbubbles.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.wynnbubbles.accessor.AbstractClientPlayerEntityAccessor;
import net.wynnbubbles.accessor.PlayerEntityRenderStateAccessor;
import net.wynnbubbles.util.RenderBubble;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;" +
                    "Lnet/minecraft/client/util/math/MatrixStack;" +
                    "Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL")
    )
    private void onRenderTail(
            LivingEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (!(state instanceof PlayerEntityRenderState playerState)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        var entity = client.world.getEntityById(playerState.id);
        if (!(entity instanceof AbstractClientPlayerEntity player)) return;

        List<String> bubbleText = ((AbstractClientPlayerEntityAccessor) player).getChatText();
        if (bubbleText == null || bubbleText.isEmpty()) {
            return;
        }

        if (client.world == null) {
            return;
        }

        RenderBubble.renderBubble(
                matrices,
                vertexConsumers,
                client.textRenderer,
                client.getEntityRenderDispatcher(),
                bubbleText,
                ((AbstractClientPlayerEntityAccessor) player).getWidth(),
                ((AbstractClientPlayerEntityAccessor) player).getHeight(),
                player.getHeight(),
                light,
                (AbstractClientPlayerEntityAccessor) player
        );
    }
}
