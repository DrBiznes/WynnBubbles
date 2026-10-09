package net.wynnbubbles.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import net.wynnbubbles.client.NametagStackTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feeds everything submitted above a player's head to {@link NametagStackTracker}. */
@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin {

    @Inject(method = "submitNameTag", at = @At("HEAD"))
    private void wynnbubbles$trackNameTag(
            PoseStack poseStack,
            Vec3 attachment,
            int yOffset,
            Component text,
            boolean seeThrough,
            int light,
            double distanceSq,
            CameraRenderState camera,
            CallbackInfo ci) {
        if (NametagStackTracker.isActive()) NametagStackTracker.onNameTag(poseStack, attachment, yOffset);
    }

    @Inject(method = "submitText", at = @At("HEAD"))
    private void wynnbubbles$trackText(
            PoseStack poseStack,
            float x,
            float y,
            FormattedCharSequence text,
            boolean dropShadow,
            Font.DisplayMode displayMode,
            int light,
            int color,
            int backgroundColor,
            int outlineColor,
            CallbackInfo ci) {
        if (NametagStackTracker.isActive()) NametagStackTracker.onText(poseStack, x, y);
    }

    @Inject(method = "submitCustomGeometry", at = @At("HEAD"))
    private void wynnbubbles$trackGeometry(
            PoseStack poseStack,
            RenderType renderType,
            SubmitNodeCollector.CustomGeometryRenderer renderer,
            CallbackInfo ci) {
        if (NametagStackTracker.isActive()) NametagStackTracker.onGeometry(poseStack, renderer);
    }
}
