package net.wynnbubbles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.wynnbubbles.WynnBubbles;
import net.wynnbubbles.bubble.Bubble;
import net.wynnbubbles.client.BubbleLayout.Box;
import net.wynnbubbles.client.BubbleLayout.TextSize;
import net.wynnbubbles.config.WynnBubblesConfig;

/**
 * Submits bubbles through the same collector nametags go through, so they are drawn in the same
 * pass and sorted with everything else instead of being drawn immediately.
 */
public final class BubbleRenderer {
    private static final Identifier TEX_NORMAL = Identifier.parse("wynnbubbles:textures/gui/background_normal.png");
    private static final Identifier TEX_PARTY = Identifier.parse("wynnbubbles:textures/gui/background_party.png");
    private static final Identifier TEX_GUILD = Identifier.parse("wynnbubbles:textures/gui/background_guild.png");
    private static final Identifier TEX_PRIVATE = Identifier.parse("wynnbubbles:textures/gui/background_private.png");

    private static final float PIXEL = 0.025f;
    private static final float TEXTURE_SIZE = 32f;
    /** Source size of a corner in the texture; it is drawn {@link BubbleLayout#CORNER} wide. */
    private static final int SRC_CORNER = 6;
    private static final int C = BubbleLayout.CORNER;

    /**
     * @param anchorY height above the entity origin of the tip of the lowest bubble's arrow
     */
    public static void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            Font font,
            List<Bubble> newestFirst,
            float anchorY) {
        WynnBubblesConfig config = WynnBubbles.CONFIG;

        int count = Math.min(newestFirst.size(), Math.max(1, config.maxBubbles));
        List<List<FormattedCharSequence>> wrapped = new ArrayList<>(count);
        List<TextSize> sizes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            List<FormattedCharSequence> lines =
                    font.split(newestFirst.get(i).text(), Math.max(20, config.maxChatWidth));
            int width = 0;
            for (FormattedCharSequence line : lines) width = Math.max(width, font.width(line));
            wrapped.add(lines);
            sizes.add(new TextSize(width, lines.size()));
        }
        List<Box> boxes = BubbleLayout.stack(sizes, config.balloonPadding, config.distanceBetweenBubbles);

        int tint = argb(config.backgroundOpacity, config.backgroundRed, config.backgroundGreen, config.backgroundBlue);
        int textColor = 0xFF000000 | config.chatColor;

        poseStack.pushPose();
        poseStack.translate(0.0f, anchorY, 0.0f);
        poseStack.mulPose(camera.orientation);
        float scale = PIXEL * config.chatScale;
        poseStack.scale(scale, -scale, scale);

        for (int i = 0; i < count; i++) {
            Box box = boxes.get(i);
            boolean withArrow = i == 0;
            Identifier texture =
                    switch (newestFirst.get(i).type()) {
                        case PARTY -> TEX_PARTY;
                        case GUILD -> TEX_GUILD;
                        case PRIVATE -> TEX_PRIVATE;
                        default -> TEX_NORMAL;
                    };
            collector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.text(texture),
                    (pose, consumer) -> drawBackground(pose, consumer, box, withArrow, tint));

            // a hair in front of the background so the two never z-fight
            poseStack.pushPose();
            poseStack.translate(0.0f, 0.0f, 0.2f);
            int y = box.textY();
            for (FormattedCharSequence line : wrapped.get(i)) {
                float x = -font.width(line) / 2f + 0.5f;
                collector.submitText(
                        poseStack, x, y, line, false, Font.DisplayMode.NORMAL, LightTexture.FULL_BRIGHT, textColor, 0, 0);
                y += BubbleLayout.LINE_HEIGHT;
            }
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void drawBackground(
            PoseStack.Pose pose, VertexConsumer consumer, Box box, boolean withArrow, int tint) {
        int x0 = box.x();
        int x1 = x0 + C;
        int x2 = x0 + box.width() - C;
        int y0 = box.y();
        int y1 = y0 + C;
        int y2 = y0 + box.height() - C;
        int midW = x2 - x1;
        int midH = y2 - y1;

        // Texture layout: 6px corners at (0,0) (14,0) (0,9) (14,9), edges between them, 1px-tall
        // middle row at v=7. The right-hand middle strip is clipped by the arrow sprite, so the
        // right edge reuses the left one mirrored.
        quad(pose, consumer, x0, y0, C, C, 0, 0, SRC_CORNER, SRC_CORNER, tint);
        quad(pose, consumer, x1, y0, midW, C, 7, 0, SRC_CORNER, SRC_CORNER, tint);
        quad(pose, consumer, x2, y0, C, C, 14, 0, SRC_CORNER, SRC_CORNER, tint);

        quad(pose, consumer, x0, y1, C, midH, 0, 7, SRC_CORNER, 1, tint);
        quad(pose, consumer, x1, y1, midW, midH, 7, 7, SRC_CORNER, 1, tint);
        quad(pose, consumer, x2, y1, C, midH, SRC_CORNER, 7, -SRC_CORNER, 1, tint);

        quad(pose, consumer, x0, y2, C, C, 0, 9, SRC_CORNER, SRC_CORNER, tint);
        quad(pose, consumer, x1, y2, midW, C, 7, 9, SRC_CORNER, SRC_CORNER, tint);
        quad(pose, consumer, x2, y2, C, C, 14, 9, SRC_CORNER, SRC_CORNER, tint);

        if (withArrow) {
            // the arrow sprite's top row is the bubble's own border, so only the rows below it are drawn
            int rows = BubbleLayout.ARROW_HEIGHT - 1;
            int width = BubbleLayout.ARROW_WIDTH;
            quad(pose, consumer, -width / 2, box.bottom(), width, rows, 18, 7, width, rows, tint);
        }
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int x,
            int y,
            int width,
            int height,
            float u,
            float v,
            float uWidth,
            float vHeight,
            int color) {
        if (width <= 0 || height <= 0) return;
        float u0 = u / TEXTURE_SIZE;
        float u1 = (u + uWidth) / TEXTURE_SIZE;
        float v0 = v / TEXTURE_SIZE;
        float v1 = (v + vHeight) / TEXTURE_SIZE;
        int light = LightTexture.FULL_BRIGHT;
        consumer.addVertex(pose, x, y, 0).setColor(color).setUv(u0, v0).setLight(light);
        consumer.addVertex(pose, x, y + height, 0).setColor(color).setUv(u0, v1).setLight(light);
        consumer.addVertex(pose, x + width, y + height, 0).setColor(color).setUv(u1, v1).setLight(light);
        consumer.addVertex(pose, x + width, y, 0).setColor(color).setUv(u1, v0).setLight(light);
    }

    private static int argb(float alpha, float red, float green, float blue) {
        return channel(alpha) << 24 | channel(red) << 16 | channel(green) << 8 | channel(blue);
    }

    private static int channel(float value) {
        return Mth.clamp(Math.round(value * 255f), 0, 255);
    }

    private BubbleRenderer() {}
}
