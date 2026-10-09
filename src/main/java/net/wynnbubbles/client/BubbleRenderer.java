package net.wynnbubbles.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.wynnbubbles.WynnBubbles;
import net.wynnbubbles.compat.WynntilsCompat;
import net.wynnbubbles.util.BubbleMessage;
import net.wynnbubbles.util.HistoricalData;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public final class BubbleRenderer {

    private static final Minecraft CLIENT = Minecraft.getInstance();

    private static final BlendFunction BUBBLE_BLEND = new BlendFunction(
            SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
            SourceFactor.ONE, DestFactor.ZERO
    );

    private static final RenderPipeline.Snippet BUBBLE_SNIPPET = RenderPipeline.builder()
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .buildSnippet();

    private static final RenderPipeline MAIN_BUBBLE_PIPELINE = RenderPipeline.builder(BUBBLE_SNIPPET)
            .withLocation("pipeline/wynnbubbles_main")
            .withDepthBias(3.0f, 3.0f)
            .withBlend(BUBBLE_BLEND)
            .build();

    /** Arrow uses no depth bias so it renders cleanly in front of the bubble background. */
    private static final RenderPipeline ARROW_PIPELINE = RenderPipeline.builder(BUBBLE_SNIPPET)
            .withLocation("pipeline/wynnbubbles_arrow")
            .withDepthBias(0.0f, 0.0f)
            .withBlend(BUBBLE_BLEND)
            .build();

    // WynnBubbles chat-type textures
    private static final Identifier TEX_NORMAL  = Identifier.parse("wynnbubbles:textures/gui/backgroundNormal.png");
    private static final Identifier TEX_PARTY   = Identifier.parse("wynnbubbles:textures/gui/backgroundParty.png");
    private static final Identifier TEX_GUILD   = Identifier.parse("wynnbubbles:textures/gui/backgroundGuild.png");
    private static final Identifier TEX_PRIVATE = Identifier.parse("wynnbubbles:textures/gui/backgroundFriends.png");

    /**
     * Renders stacked chat bubbles above a player.
     *
     * @param poseStack       world-space pose stack (from LivingEntityRendererMixin)
     * @param cameraOrientation camera orientation quaternion (from CameraRenderState)
     * @param font            client font renderer
     * @param messages        the player's bubble message history (newest = index 0)
     * @param playerHeight    translation base above entity feet (boundingBoxHeight + small offset)
     */
    public static void renderBubbles(
            PoseStack poseStack,
            Quaternionf cameraOrientation,
            Font font,
            HistoricalData<BubbleMessage> messages,
            float playerHeight
    ) {
        if (messages == null || messages.isEmpty()) return;

        // Camera-facing rotation — yaw only, matching Talk-Balloons' approach
        Quaternionf rotation = Axis.YP.rotationDegrees(toEulerXyzDegrees(cameraOrientation).y() - 180f);

        int balloonDistance = 0;
        int previousBalloonHeight = 0;
        int padding = WynnBubbles.CONFIG.balloonPadding;

        var renderTarget = CLIENT.getMainRenderTarget();
        var encoder = RenderSystem.getDevice().createCommandEncoder();

        int renderCount = Math.min(messages.size(), WynnBubbles.CONFIG.maxBubbles);
        for (int i = 0; i < renderCount; i++) {
            BubbleMessage msg = messages.get(i);

            Identifier texId = switch (msg.chatType()) {
                case PARTY   -> TEX_PARTY;
                case GUILD   -> TEX_GUILD;
                case PRIVATE -> TEX_PRIVATE;
                default      -> TEX_NORMAL;
            };

            var texture        = CLIENT.getTextureManager().getTexture(texId);
            var gpuTexture     = texture.getTextureView();
            var textureSampler = texture.getSampler();

            BufferBuilder builder = Tesselator.getInstance().begin(
                    VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            poseStack.pushPose();
            poseStack.translate(0.0, playerHeight + WynnBubbles.CONFIG.balloonsHeightOffset + WynnBubbles.CONFIG.chatHeight + WynntilsCompat.getOffset(), 0.0);
            poseStack.mulPose(rotation);
            poseStack.scale(
                    -0.025f * WynnBubbles.CONFIG.chatScale,
                    -0.025f * WynnBubbles.CONFIG.chatScale,
                     0.025f);

            // Write per-draw color tint into the DynamicTransforms uniform buffer
            var dynamicTransforms = RenderSystem.getDynamicUniforms()
                    .writeTransform(
                            RenderSystem.getModelViewMatrix(),
                            new Vector4f(
                                    WynnBubbles.CONFIG.backgroundRed,
                                    WynnBubbles.CONFIG.backgroundGreen,
                                    WynnBubbles.CONFIG.backgroundBlue,
                                    WynnBubbles.CONFIG.backgroundOpacity),
                            new Vector3f(),
                            new Matrix4f());

            // Word-wrap via Font.split
            List<FormattedCharSequence> lines = font.split(msg.message(), WynnBubbles.CONFIG.maxChatWidth);

            int greatestTextWidth = 0;
            for (FormattedCharSequence line : lines) {
                int w = font.width(line);
                if (w > greatestTextWidth) greatestTextWidth = w;
            }

            int balloonWidth  = Mth.clamp(greatestTextWidth, 10, WynnBubbles.CONFIG.maxChatWidth);
            int balloonHeight = lines.size();

            if (balloonWidth % 2 == 0) balloonWidth--; // keep width odd for centred arrow (Phase 7)

            // Accumulate vertical stacking offset for each previous balloon
            if (previousBalloonHeight != 0)
                balloonDistance += 9 * previousBalloonHeight + WynnBubbles.CONFIG.distanceBetweenBubbles + (padding * 2);
            previousBalloonHeight = balloonHeight;

            int j     = balloonHeight - 1;
            int baseX = balloonWidth / 2;
            int baseY = (-balloonHeight - j * 7) - j; // top-left Y of the text block

            // --- 9-patch background ---
            // UV layout (WynnBubbles 6-px corners): U columns 0,7,14 | V rows 0,7,9
            // Left column
            blit(poseStack.last(), builder, -baseX - 3 - padding, baseY - balloonDistance - padding,      5, 5,  0f, 0f, 6, 6, 32, 32);
            blit(poseStack.last(), builder, -baseX - 3 - padding, baseY + 5 - balloonDistance - padding,  5, balloonHeight + j * 8 + padding * 2, 0f, 7f, 6, 1, 32, 32);
            blit(poseStack.last(), builder, -baseX - 3 - padding, 5 - balloonDistance + padding,          5, 5,  0f, 9f, 6, 6, 32, 32);

            // Middle column
            blit(poseStack.last(), builder, -baseX + 2 - padding, baseY - balloonDistance - padding,     balloonWidth - 4 + padding * 2, 5,  7f, 0f, 6, 6, 32, 32);
            blit(poseStack.last(), builder, -baseX + 2 - padding, baseY + 5 - balloonDistance - padding, balloonWidth - 4 + padding * 2, balloonHeight + j * 8 + padding * 2, 7f, 7f, 6, 1, 32, 32);
            blit(poseStack.last(), builder, -baseX + 2 - padding, 5 - balloonDistance + padding,         balloonWidth - 4 + padding * 2, 5,  7f, 9f, 6, 6, 32, 32);

            // Right column
            blit(poseStack.last(), builder, baseX - 1 + padding, baseY - balloonDistance - padding,      5, 5,  14f, 0f, 6, 6, 32, 32);
            blit(poseStack.last(), builder, baseX - 1 + padding, baseY + 5 - balloonDistance - padding,  5, balloonHeight + j * 8 + padding * 2, 14f, 7f, 6, 1, 32, 32);
            blit(poseStack.last(), builder, baseX - 1 + padding, 5 - balloonDistance + padding,          5, 5,  14f, 9f, 6, 6, 32, 32);

            // Arrow — only on the newest (bottom-most) bubble, centred below
            boolean drawArrow = (i == 0);
            if (drawArrow) {
                blit(poseStack.last(), builder, -3, 9 + padding - balloonDistance, 7, 4, 18f, 6f, 7, 4, 32, 32);
            }

            // Upload 9-patch + optional arrow into a single vertex buffer, then draw
            // with two pipeline calls: depth-biased for 9-patch, unbiased for arrow.
            try (MeshData meshData = builder.buildOrThrow()) {
                var vertexBuffer       = DefaultVertexFormat.POSITION_TEX_COLOR.uploadImmediateVertexBuffer(meshData.vertexBuffer());
                var indexBufferStorage = RenderSystem.getSequentialBuffer(meshData.drawState().mode());
                var indexBuffer        = indexBufferStorage.getBuffer(meshData.drawState().indexCount());
                var indexType          = indexBufferStorage.type();

                try (RenderPass pass = encoder.createRenderPass(
                        () -> "WynnBubbles bubble",
                        renderTarget.getColorTextureView(), OptionalInt.empty(),
                        renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
                    pass.bindTexture("Sampler0", gpuTexture, textureSampler);
                    RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms", dynamicTransforms);
                    pass.setVertexBuffer(0, vertexBuffer);
                    pass.setIndexBuffer(indexBuffer, indexType);
                    pass.setPipeline(MAIN_BUBBLE_PIPELINE);
                    pass.drawIndexed(0, 0, 9 * 6, 0);       // 9 quads — depth-biased background
                    if (drawArrow) {
                        pass.setPipeline(ARROW_PIPELINE);
                        pass.drawIndexed(0, 9 * 6, 1 * 6, 0); // 1 quad — unbiased arrow
                    }
                }
            }

            // Reset tint to opaque white so text renders unmodified
            RenderSystem.getDynamicUniforms()
                    .writeTransform(RenderSystem.getModelViewMatrix(),
                            new Vector4f(1f, 1f, 1f, 1f), new Vector3f(), new Matrix4f());

            // --- Text ---
            int textColor = ARGB.opaque(WynnBubbles.CONFIG.chatColor);
            if (lines.size() > 1) {
                int textOffset = 0;
                for (FormattedCharSequence line : lines) {
                    font.drawInBatch(
                            line,
                            -font.width(line) / 2f + 1f,
                            -(9 * balloonHeight - 10) - balloonDistance + textOffset,
                            textColor, false,
                            poseStack.last().pose(),
                            CLIENT.renderBuffers().bufferSource(),
                            Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
                    textOffset += 9;
                }
            } else {
                font.drawInBatch(
                        lines.get(0),
                        -greatestTextWidth / 2f + 1f,
                        balloonHeight - balloonDistance,
                        textColor, false,
                        poseStack.last().pose(),
                        CLIENT.renderBuffers().bufferSource(),
                        Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            }

            poseStack.popPose();
        }
    }

    /**
     * Emits a single quad into the builder using a sub-region of the texture atlas.
     * UV layout uses floating-point offsets to support non-power-of-two regions.
     */
    private static void blit(
            PoseStack.Pose pose, VertexConsumer consumer,
            int x, int y, int width, int height,
            float uOffset, float vOffset, int uWidth, int vHeight,
            int textureWidth, int textureHeight
    ) {
        var matrix = pose.pose();
        int x2 = x + width;
        int y2 = y + height;
        float minU = uOffset / textureWidth;
        float maxU = (uOffset + uWidth) / textureWidth;
        float minV = vOffset / textureHeight;
        float maxV = (vOffset + vHeight) / textureHeight;

        consumer.addVertex(matrix, (float) x,  (float) y,  0f).setUv(minU, minV).setColor(-1);
        consumer.addVertex(matrix, (float) x,  (float) y2, 0f).setUv(minU, maxV).setColor(-1);
        consumer.addVertex(matrix, (float) x2, (float) y2, 0f).setUv(maxU, maxV).setColor(-1);
        consumer.addVertex(matrix, (float) x2, (float) y,  0f).setUv(maxU, minV).setColor(-1);
    }

    /** Converts a quaternion to Euler angles in degrees (XYZ order). */
    private static Vector3f toEulerXyzDegrees(Quaternionf quaternionf) {
        float w = quaternionf.w(), x = quaternionf.x(), y = quaternionf.y(), z = quaternionf.z();
        float sum  = w * w + x * x + y * y + z * z;
        float sinX = 2.0f * w * x - 2.0f * y * z;
        float angX = (float) Math.asin(sinX / sum);

        if (Math.abs(sinX) > 0.999f * sum) {
            return new Vector3f(
                    (float) Math.toDegrees(angX),
                    (float) Math.toDegrees(2.0f * Math.atan2(y, w)),
                    0.0f);
        }
        return new Vector3f(
                (float) Math.toDegrees(angX),
                (float) Math.toDegrees(Math.atan2(2.0f * x * z + 2.0f * y * w, sum - 2 * x * x - 2 * y * y)),
                (float) Math.toDegrees(Math.atan2(2.0f * x * y + 2.0f * w * z, sum - 2 * x * x - 2 * z * z)));
    }

    private BubbleRenderer() {}
}
