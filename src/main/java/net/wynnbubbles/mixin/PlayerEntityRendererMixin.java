package net.wynnbubbles.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.wynnbubbles.WynnBubbles;
import net.wynnbubbles.accessor.AbstractClientPlayerEntityAccessor;
import net.wynnbubbles.util.RenderBubble;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public PlayerEntityRendererMixin(Context ctx, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void renderMixin(AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i,
                             CallbackInfo info) {
        if (!abstractClientPlayerEntity.isInvisible() && abstractClientPlayerEntity.isAlive()) {
            AbstractClientPlayerEntityAccessor entityAccessor = (AbstractClientPlayerEntityAccessor) abstractClientPlayerEntity;
            int oldAge = entityAccessor.getOldAge();
            if (oldAge != 0 && oldAge != -1) {
                if (abstractClientPlayerEntity.age - oldAge > WynnBubbles.CONFIG.chatTime)
                    entityAccessor.setChatText(null, 0, 0, 0, RenderBubble.ChatType.NORMAL);
                List<String> textList = entityAccessor.getChatText();
                if (textList != null && !textList.isEmpty()) {
                    RenderBubble.renderBubble(matrixStack, vertexConsumerProvider, this.getTextRenderer(), this.dispatcher, textList,
                            entityAccessor.getWidth(), entityAccessor.getHeight(),
                            abstractClientPlayerEntity.getHeight(), i, entityAccessor);
                }
            }
        }
    }
}