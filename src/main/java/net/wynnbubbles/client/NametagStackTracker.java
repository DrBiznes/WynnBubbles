package net.wynnbubbles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Finds the top of whatever is stacked above a player's head while that player is being submitted.
 * It watches the submit collector itself rather than any one mod, so vanilla nametags, Wynntils'
 * extra lines and badges, and rows added by other mods are all measured the same way, as long as
 * they are submitted during the player's own submit call.
 */
public final class NametagStackTracker {
    /** Nametags are scaled by this, and drawn half a block above their attachment point. */
    private static final float PIXEL = 0.025f;
    private static final float NAMETAG_LIFT = 0.5f;
    /** Anything further above the head than this is an effect, not a nametag row. */
    private static final float MAX_STACK_HEIGHT = 4.0f;

    private static final Measurer MEASURER = new Measurer();

    private static boolean active;
    private static float baseY;
    private static float headY;
    private static float top;

    public static void begin(PoseStack poseStack, float headHeight) {
        active = true;
        baseY = poseStack.last().pose().m31();
        headY = headHeight;
        top = Float.NaN;
    }

    /** @return height above the entity origin of the highest thing seen, or NaN if there was none */
    public static float end() {
        active = false;
        return top;
    }

    public static boolean isActive() {
        return active;
    }

    public static void onNameTag(PoseStack poseStack, Vec3 attachment, int yOffset) {
        if (attachment == null) return;
        Vector3f origin = poseStack.last().pose().transformPosition(
                (float) attachment.x, (float) attachment.y + NAMETAG_LIFT, (float) attachment.z, new Vector3f());
        // the background starts one pixel above the text, and yOffset moves the text down
        record(origin.y + (1 - yOffset) * PIXEL);
    }

    public static void onText(PoseStack poseStack, float x, float y) {
        record(poseStack.last().pose().transformPosition(x, y, 0, new Vector3f()).y);
    }

    public static void onGeometry(PoseStack poseStack, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        MEASURER.maxY = Float.NaN;
        try {
            renderer.render(poseStack.last(), MEASURER);
        } catch (RuntimeException e) {
            return;
        }
        if (!Float.isNaN(MEASURER.maxY)) record(MEASURER.maxY);
    }

    private static void record(float cameraRelativeY) {
        float height = cameraRelativeY - baseY;
        if (height < headY || height > headY + MAX_STACK_HEIGHT) return;
        if (Float.isNaN(top) || height > top) top = height;
    }

    /** Runs a geometry callback without drawing anything, to see how high it reaches. */
    private static final class Measurer implements VertexConsumer {
        private float maxY;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (Float.isNaN(maxY) || y > maxY) maxY = y;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }

    private NametagStackTracker() {}
}
