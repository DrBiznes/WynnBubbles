package net.wynnbubbles.util;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.wynnbubbles.WynnBubbles;
import net.wynnbubbles.mixin.DrawContextAccessor;

@Environment(EnvType.CLIENT)
public class RenderBubble {
    private static final Identifier BACKGROUND = Identifier.of("wynnbubbles:textures/gui/background.png");

    // Store the last detected chat type
    private static ChatType lastChatType = ChatType.NORMAL;

    private static final String PARTY_CREATION_MESSAGE = "You have successfully created a party";

    private static final int[][] GUILD_SEQUENCES = {
            // Main guild sequence
            {0xDAFF, 0xDFFC, 0xE006, 0xDAFF, 0xDFFF, 0xE002, 0xDAFF, 0xDFFE}
    };

    private static final int[][] PARTY_SEQUENCES = {
            // Main party sequence
            {0xDAFF, 0xDFFC, 0xE005, 0xDAFF, 0xDFFF, 0xE002, 0xDAFF, 0xDFFE}
    };

    private static boolean matchesSequence(String text, int[] sequence) {
        if (text == null || text.isEmpty() || text.length() < sequence.length) return false;

        for (int i = 0; i < sequence.length; i++) {
            if (text.charAt(i) != sequence[i]) return false;
        }
        return true;
    }

    private static boolean matchesAnySequence(String text, int[][] sequences) {
        for (int[] sequence : sequences) {
            if (matchesSequence(text, sequence)) return true;
        }
        return false;
    }

    private static ChatType getChatType(String text) {
        System.out.println("RenderBubble: Analyzing message for chat type: " + text);

        // Check for party creation message
        if (text.contains(PARTY_CREATION_MESSAGE)) {
            System.out.println("RenderBubble: Party creation message detected - setting lastChatType to PARTY");
            lastChatType = ChatType.PARTY;
            return ChatType.PARTY;
        }

        // Check for guild chat sequence
        if (matchesAnySequence(text, GUILD_SEQUENCES)) {
            System.out.println("RenderBubble: Guild chat sequence detected");
            return ChatType.GUILD;
        }

        // Check for party chat sequence
        if (matchesAnySequence(text, PARTY_SEQUENCES)) {
            System.out.println("RenderBubble: Party chat sequence detected");
            return ChatType.PARTY;
        }

        System.out.println("RenderBubble: Normal chat detected");
        return ChatType.NORMAL;
    }

    private enum ChatType {
        NORMAL,
        PARTY,
        GUILD
    }

    public static void renderBubble(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, TextRenderer textRenderer, EntityRenderDispatcher entityRenderDispatcher,
                                    List<String> textList, int width, int height, float playerHeight, int i) {
        matrixStack.push();

        int backgroundWidth = width;
        int backgroundHeight = height;

        matrixStack.translate(0.0D, playerHeight + 0.9F + (backgroundHeight > 5 ? 0.1F : 0.0F) + WynnBubbles.CONFIG.chatHeight, 0.0D);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(toEulerXyzDegrees(entityRenderDispatcher.getRotation()).y()));
        matrixStack.scale(0.025F * WynnBubbles.CONFIG.chatScale, -0.025F * WynnBubbles.CONFIG.chatScale, 0.025F);

        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

        // Determine chat type from the first message (which contains the full raw message)
        ChatType chatType = ChatType.NORMAL;
        if (!textList.isEmpty()) {
            chatType = getChatType(textList.get(0));
            System.out.println("RenderBubble: Final chat type determined: " + chatType);
        }

        // Set color based on chat type
        float red, green, blue;
        switch (chatType) {
            case PARTY:
                red = 1.0f;
                green = 1.0f;
                blue = 0.0f; // Yellow for party
                System.out.println("RenderBubble: Using PARTY colors (yellow)");
                break;
            case GUILD:
                red = 0.3333f;
                green = 1.0f;
                blue = 1.0f; // Aqua for guild
                System.out.println("RenderBubble: Using GUILD colors (aqua)");
                break;
            default:
                red = WynnBubbles.CONFIG.backgroundRed;
                green = WynnBubbles.CONFIG.backgroundGreen;
                blue = WynnBubbles.CONFIG.backgroundBlue;
                System.out.println("RenderBubble: Using normal colors");
                break;
        }

        RenderSystem.setShaderColor(red, green, blue, WynnBubbles.CONFIG.backgroundOpacity);

        // Standard rendering setup
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(3.0F, 3.0F);
        MinecraftClient client = MinecraftClient.getInstance();
        DrawContext context = DrawContextAccessor.getDrawContext(client, matrixStack, client.getBufferBuilders().getEntityVertexConsumers());

        // Bubble geometry rendering
        context.drawTexture(BACKGROUND, -backgroundWidth / 2 - 2, -backgroundHeight - (backgroundHeight - 1) * 7, 5, 5, 0.0F, 0.0F, 5, 5, 32, 32);
        context.drawTexture(BACKGROUND, -backgroundWidth / 2 - 2, -backgroundHeight - (backgroundHeight - 1) * 7 + 5, 5, backgroundHeight + (backgroundHeight - 1) * 8, 0.0F, 6.0F, 5, 1, 32, 32);
        context.drawTexture(BACKGROUND, -backgroundWidth / 2 - 2, 5 + (backgroundHeight - 1), 5, 5, 0.0F, 8.0F, 5, 5, 32, 32);
        context.drawTexture(BACKGROUND, -backgroundWidth / 2 + 3, -backgroundHeight - (backgroundHeight - 1) * 7, backgroundWidth - 4, 5, 6.0F, 0.0F, 5, 5, 32, 32);
        context.drawTexture(BACKGROUND, -backgroundWidth / 2 + 3, -backgroundHeight - (backgroundHeight - 1) * 7 + 5, backgroundWidth - 4, backgroundHeight + (backgroundHeight - 1) * 8, 6.0F, 6.0F, 5, 1, 32, 32);
        context.drawTexture(BACKGROUND, -backgroundWidth / 2 + 3, 5 + (backgroundHeight - 1), backgroundWidth - 4, 5, 6.0F, 8.0F, 5, 5, 32, 32);
        context.drawTexture(BACKGROUND, backgroundWidth / 2 - 1, -backgroundHeight - (backgroundHeight - 1) * 7, 5, 5, 12.0F, 0.0F, 5, 5, 32, 32);
        context.drawTexture(BACKGROUND, backgroundWidth / 2 - 1, -backgroundHeight - (backgroundHeight - 1) * 7 + 5, 5, backgroundHeight + (backgroundHeight - 1) * 8, 12.0F, 6.0F, 5, 1, 32, 32);
        context.drawTexture(BACKGROUND, backgroundWidth / 2 - 1, 5 + (backgroundHeight - 1), 5, 5, 12.0F, 8.0F, 5, 5, 32, 32);

        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.disablePolygonOffset();

        // Text rendering with proper centering (skip first line as it contains the raw message)
        for (int u = textList.size(); u > 1; u--) {
            String lineText = textList.get(u - 1);
            int lineWidth = textRenderer.getWidth(lineText);
            float xOffset = (float)(-lineWidth) / 2.0F + 1.0F;

            textRenderer.draw(lineText,
                    xOffset,
                    ((float) textList.size() - 1 + (u - textList.size()) * 9),
                    WynnBubbles.CONFIG.chatColor,
                    false,
                    matrix4f,
                    vertexConsumerProvider,
                    TextRenderer.TextLayerType.NORMAL,
                    0,
                    i);
        }

        matrixStack.pop();

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
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