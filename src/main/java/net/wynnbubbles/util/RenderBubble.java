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

    // Updated sequence definitions from the original version
    private static final int[][] GUILD_SEQUENCES = {
            // Main guild sequence
            {0xDAFF, 0xDFFC, 0xE006, 0xDAFF, 0xDFFF, 0xE002, 0xDAFF, 0xDFFE},
            // Guild continuation sequence
            {0xDAFF, 0xDFFC, 0xE001, 0xDB00, 0xDC06}
    };

    private static final int[][] PARTY_SEQUENCES = {
            // Main party sequence
            {0xDAFF, 0xDFFC, 0xE005, 0xDAFF, 0xDFFF, 0xE002, 0xDAFF, 0xDFFE},
            // Party continuation sequence
            {0xDAFF, 0xDFFC, 0xE001, 0xDB00, 0xDC06}
    };

    private static void debugPrintCharacters(String text) {
        if (text == null || text.isEmpty()) return;
        System.out.println("\nDebug: Analyzing message");
        System.out.println("Message length: " + text.length());
        System.out.print("First characters (hex): ");
        for (int i = 0; i < Math.min(text.length(), 20); i++) {
            System.out.printf("0x%04X ", (int)text.charAt(i));
        }
        System.out.println("\nRaw text: " + text);
    }

    private static boolean matchesAnySequence(String text, int[][] sequences) {
        if (text == null || text.isEmpty()) return false;

        for (int[] sequence : sequences) {
            if (text.length() < sequence.length) continue;

            boolean matches = true;
            for (int i = 0; i < sequence.length; i++) {
                if (text.charAt(i) != sequence[i]) {
                    matches = false;
                    break;
                }
            }
            if (matches) return true;
        }
        return false;
    }

    private static ChatType getChatType(String text) {
        debugPrintCharacters(text);

        // Check for guild chat sequences
        if (matchesAnySequence(text, GUILD_SEQUENCES)) {
            System.out.println("Detected GUILD chat");
            return ChatType.GUILD;
        }

        // Check for party chat sequences
        if (matchesAnySequence(text, PARTY_SEQUENCES)) {
            System.out.println("Detected PARTY chat");
            return ChatType.PARTY;
        }

        System.out.println("Detected NORMAL chat");
        return ChatType.NORMAL;
    }

    private enum ChatType {
        NORMAL,
        PARTY,
        GUILD
    }

    public static void renderBubble(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider,
                                    TextRenderer textRenderer, EntityRenderDispatcher entityRenderDispatcher,
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
        }

        // Set color based on chat type
        float red, green, blue;
        switch (chatType) {
            case PARTY:
                red = 1.0f; green = 1.0f; blue = 0.0f; // Yellow for party
                break;
            case GUILD:
                red = 0.3333f; green = 1.0f; blue = 1.0f; // Aqua for guild
                break;
            default:
                red = WynnBubbles.CONFIG.backgroundRed;
                green = WynnBubbles.CONFIG.backgroundGreen;
                blue = WynnBubbles.CONFIG.backgroundBlue;
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

        // Text rendering with proper centering (keeping the improved version)
        // Skip the first line as it contains the raw message with Unicode characters
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