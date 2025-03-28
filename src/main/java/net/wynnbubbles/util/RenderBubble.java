package net.wynnbubbles.util;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.TriState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.wynnbubbles.WynnBubbles;
import net.wynnbubbles.accessor.AbstractClientPlayerEntityAccessor;
import net.wynnbubbles.mixin.DrawContextAccessor;

@Environment(EnvType.CLIENT)
public class RenderBubble {
    private static final Identifier BACKGROUNDPARTY = Identifier.of("wynnbubbles:textures/gui/backgroundParty.png");
    private static final Identifier BACKGROUNDGUILD = Identifier.of("wynnbubbles:textures/gui/backgroundGuild.png");
    private static final Identifier BACKGROUNDNORMAL = Identifier.of("wynnbubbles:textures/gui/backgroundNormal.png");
    private static final Identifier BACKGROUNDFRIEND = Identifier.of("wynnbubbles:textures/gui/backgroundFriends.png");

    public static final String PARTY_CREATION_MESSAGE = "You have successfully created a party";
    public static final int[][] GUILD_SEQUENCES = {
            {0xDAFF, 0xDFFC, 0xE006, 0xDAFF, 0xDFFF, 0xE002, 0xDAFF, 0xDFFE},
    };
    public static final int[][] PARTY_SEQUENCES = {
            {0xDAFF, 0xDFFC, 0xE005, 0xDAFF, 0xDFFF, 0xE002, 0xDAFF, 0xDFFE},
    };
    public static final int[][] PRIVATE_MESSAGE_SEQUENCES = {
            {0xDAFF, 0xDFFC, 0xE007, 0xDAFF, 0xDFFF, 0xE002, 0xDAFF, 0xDFFE}
    };

    public enum ChatType {
        NORMAL,
        PARTY,
        GUILD,
        PRIVATE
    }

    public static boolean matchesSequence(String text, int[] sequence) {
        if (text == null || text.isEmpty() || text.length() < sequence.length) return false;

        for (int i = 0; i < sequence.length; i++) {
            if (text.charAt(i) != sequence[i]) return false;
        }
        return true;
    }

    public static boolean matchesAnySequence(String text, int[][] sequences) {
        for (int[] sequence : sequences) {
            if (matchesSequence(text, sequence)) return true;
        }
        return false;
    }

    public static void renderBubble(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, TextRenderer textRenderer, EntityRenderDispatcher entityRenderDispatcher,
                                    List<String> textList, int width, int height, float playerHeight, int i, AbstractClientPlayerEntityAccessor entity) {
        matrixStack.push();

        int backgroundWidth = width;
        int backgroundHeight = height;
        VertexConsumerProvider.Immediate bubbleLayerProvider =
                VertexConsumerProvider.immediate(new BufferAllocator(256));
        matrixStack.translate(0.0D, playerHeight + 0.9F + (backgroundHeight > 5 ? 0.1F : 0.0F) + WynnBubbles.CONFIG.chatHeight, 0.0D);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(toEulerXyzDegrees(entityRenderDispatcher.getRotation()).y()));
        matrixStack.scale(0.025F * WynnBubbles.CONFIG.chatScale, -0.025F * WynnBubbles.CONFIG.chatScale, 0.025F);

        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

        float red = 1.0f;
        float green = 1.0f;
        float blue = 1.0f;
        Identifier BackgroundToUse = BACKGROUNDNORMAL;
        switch (entity.getChatType()) {
            case PARTY:
                BackgroundToUse = BACKGROUNDPARTY;
                break;
            case GUILD:
                BackgroundToUse = BACKGROUNDGUILD;
                break;
            case PRIVATE:
                BackgroundToUse = BACKGROUNDFRIEND;
                break;
        }

        //RenderSystem.setShaderColor(red, green, blue, WynnBubbles.CONFIG.backgroundOpacity);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.polygonOffset(-1.0F, -10.0F); // render in front
        RenderSystem.enablePolygonOffset();

        MinecraftClient client = MinecraftClient.getInstance();
        DrawContext context = DrawContextAccessor.getDrawContext(client, matrixStack, client.getBufferBuilders().getEntityVertexConsumers());

        drawColoredTexture(matrixStack, bubbleLayerProvider, BackgroundToUse,
                -backgroundWidth / 2f - 2, -backgroundHeight - (backgroundHeight - 1) * 7,
                5, 5,
                0.0f, 0.0f, 6, 6,
                red, green, blue, WynnBubbles.CONFIG.backgroundOpacity);

        drawColoredTexture(matrixStack, bubbleLayerProvider, BackgroundToUse,
                -backgroundWidth / 2f - 2, -backgroundHeight - (backgroundHeight - 1) * 7 + 5,
                5, backgroundHeight + (backgroundHeight - 1) * 8,
                0.0f, 7.0f, 6, 1,
                red, green, blue, WynnBubbles.CONFIG.backgroundOpacity);

        drawColoredTexture(matrixStack, bubbleLayerProvider, BackgroundToUse,
                -backgroundWidth / 2f - 2, 5 + (backgroundHeight - 1),
                5, 5,
                0.0f, 9.0f, 6, 6,
                red, green, blue, WynnBubbles.CONFIG.backgroundOpacity);

        drawColoredTexture(matrixStack, bubbleLayerProvider, BackgroundToUse,
                -backgroundWidth / 2f + 3, -backgroundHeight - (backgroundHeight - 1) * 7,
                backgroundWidth - 3, 5,
                7.0f, 0.0f, 6, 6,
                red, green, blue, WynnBubbles.CONFIG.backgroundOpacity);

        drawColoredTexture(matrixStack, bubbleLayerProvider, BackgroundToUse,
                -backgroundWidth / 2f + 3, -backgroundHeight - (backgroundHeight - 1) * 7 + 5,
                backgroundWidth - 3, backgroundHeight + (backgroundHeight - 1) * 8,
                7.0f, 7.0f, 6, 1,
                red, green, blue, WynnBubbles.CONFIG.backgroundOpacity);

        drawColoredTexture(matrixStack, bubbleLayerProvider, BackgroundToUse,
                -backgroundWidth / 2f + 3, 5 + (backgroundHeight - 1),
                backgroundWidth - 3, 5,
                7.0f, 9.0f, 6, 6,
                red, green, blue, WynnBubbles.CONFIG.backgroundOpacity);

        drawColoredTexture(matrixStack, bubbleLayerProvider, BackgroundToUse,
                backgroundWidth / 2f, -backgroundHeight - (backgroundHeight - 1) * 7,
                5, 5,
                14.0f, 0.0f, 6, 6,
                red, green, blue, WynnBubbles.CONFIG.backgroundOpacity);

        drawColoredTexture(matrixStack, bubbleLayerProvider, BackgroundToUse,
                backgroundWidth / 2f, -backgroundHeight - (backgroundHeight - 1) * 7 + 5,
                5, backgroundHeight + (backgroundHeight - 1) * 8,
                14.0f, 7.0f, 6, 1,
                red, green, blue, WynnBubbles.CONFIG.backgroundOpacity);

        drawColoredTexture(matrixStack, bubbleLayerProvider, BackgroundToUse,
                backgroundWidth / 2f, 5 + (backgroundHeight - 1),
                5, 5,
                14.0f, 9.0f, 6, 6,
                red, green, blue, WynnBubbles.CONFIG.backgroundOpacity);

        bubbleLayerProvider.draw();

        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.disablePolygonOffset();

        for (int u = textList.size(); u > 1; u--) {
            String lineText = textList.get(u - 1);
            int lineWidth = textRenderer.getWidth(lineText);
            float xOffset = (float)(-lineWidth) / 2.0F + 2.0F;
            textRenderer.draw(lineText,
                    xOffset,
                    ((float) textList.size() - 1 + (u - textList.size()) * 9),
                    WynnBubbles.CONFIG.chatColor,
                    false,
                    matrix4f,
                    vertexConsumerProvider,
                    TextRenderer.TextLayerType.SEE_THROUGH,
                    0,
                    i);
        }
        RenderSystem.enableDepthTest();
        matrixStack.pop();

        RenderSystem.disableBlend();
        //RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void drawColoredTexture(MatrixStack matrices, VertexConsumerProvider provider, Identifier texture,
                                           float x, float y, float width, float height,
                                           float u, float v, float uSize, float vSize,
                                           float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vc = provider.getBuffer(RenderLayer.getText(texture));

        float u0 = u / 32.0f;
        float v0 = v / 32.0f;
        float u1 = (u + uSize) / 32.0f;
        float v1 = (v + vSize) / 32.0f;

        int overlay = OverlayTexture.DEFAULT_UV;
        int light = 15728880; // full brightness

        vc.vertex(matrix, x, y + height, 0).color(r, g, b, a).texture(u0, v1).overlay(overlay).light(light).normal(0, 0, 1);
        vc.vertex(matrix, x + width, y + height, 0).color(r, g, b, a).texture(u1, v1).overlay(overlay).light(light).normal(0, 0, 1);
        vc.vertex(matrix, x + width, y, 0).color(r, g, b, a).texture(u1, v0).overlay(overlay).light(light).normal(0, 0, 1);
        vc.vertex(matrix, x, y, 0).color(r, g, b, a).texture(u0, v0).overlay(overlay).light(light).normal(0, 0, 1);
    }


    private static Vector3f toEulerXyz(Quaternionf quaternionf) {
        float f = quaternionf.w() * quaternionf.w();
        float g = quaternionf.x() * quaternionf.x();
        float h = quaternionf.y() * quaternionf.y();
        float i = quaternionf.z() * quaternionf.z();
        float j = f + g + h + i;
        float k = 2.0f * quaternionf.w() * quaternionf.x() - 2.0f * quaternionf.y() * quaternionf.z();
        float l = (float) Math.asin(k / j);
        if (Math.abs(k) > 0.999f * j) {
            return new Vector3f(l, 2.0f * (float) Math.atan2(quaternionf.y(), quaternionf.w()), 0.0f);
        }
        return new Vector3f(l, (float) Math.atan2(2.0f * quaternionf.x() * quaternionf.z() + 2.0f * quaternionf.y() * quaternionf.w(), f - g - h + i),
                (float) Math.atan2(2.0f * quaternionf.x() * quaternionf.y() + 2.0f * quaternionf.w() * quaternionf.z(), f - g + h - i));
    }

    private static Vector3f toEulerXyzDegrees(Quaternionf quaternionf) {
        Vector3f vec3f = RenderBubble.toEulerXyz(quaternionf);
        return new Vector3f((float) Math.toDegrees(vec3f.x()), (float) Math.toDegrees(vec3f.y()), (float) Math.toDegrees(vec3f.z()));
    }
}