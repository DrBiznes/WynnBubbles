package net.wynnbubbles.test.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.wynnbubbles.test.TwoPlayerBubbleTest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stands in for other mods that decorate player nametags: a title row above the name (the way a
 * titles mod or Wynntils' extra lines would add one) and a badge quad above that (the way Wynntils
 * draws leaderboard badges). WynnBubbles knows nothing about this mixin; bubbles should still end
 * up above both.
 */
@Mixin(AvatarRenderer.class)
public class ExtraNametagRowsMixin {
    private static final Identifier BADGE = Identifier.parse("wynnbubbles:textures/gui/background_party.png");

    @Inject(method = "submitNameTag", at = @At("HEAD"))
    private void addRows(
            AvatarRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo ci) {
        if (!TwoPlayerBubbleTest.extraNametagRows || state.nameTagAttachment == null || state.nameTag == null) return;

        Vec3 titlePos = state.nameTagAttachment.add(0, 0.26, 0);
        collector.submitNameTag(
                poseStack,
                titlePos,
                0,
                Component.literal("The Legend"),
                !state.isDiscrete,
                state.lightCoords,
                state.distanceToCameraSq,
                camera);

        poseStack.pushPose();
        poseStack.translate(state.nameTagAttachment.x, state.nameTagAttachment.y + 0.5 + 0.35, state.nameTagAttachment.z);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(0.025f, -0.025f, 0.025f);
        collector.submitCustomGeometry(poseStack, RenderTypes.text(BADGE), (pose, consumer) -> {
            // a 6x6 badge whose top edge is 15 pixels above its own origin
            consumer.addVertex(pose, -3, -15, 0).setColor(-1).setUv(0, 0).setLight(LightTexture.FULL_BRIGHT);
            consumer.addVertex(pose, -3, -9, 0).setColor(-1).setUv(0, 6 / 32f).setLight(LightTexture.FULL_BRIGHT);
            consumer.addVertex(pose, 3, -9, 0).setColor(-1).setUv(6 / 32f, 6 / 32f).setLight(LightTexture.FULL_BRIGHT);
            consumer.addVertex(pose, 3, -15, 0).setColor(-1).setUv(6 / 32f, 0).setLight(LightTexture.FULL_BRIGHT);
        });
        poseStack.popPose();
    }
}
