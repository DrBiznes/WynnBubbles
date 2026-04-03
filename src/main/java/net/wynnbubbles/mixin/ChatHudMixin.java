package net.wynnbubbles.mixin;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntitySelector;
import net.wynnbubbles.WynnBubbles;
import net.wynnbubbles.accessor.AbstractClientPlayerEntityAccessor;
import net.wynnbubbles.util.BubbleMessage;
import net.wynnbubbles.util.ChatType;
import net.wynnbubbles.util.WynnChatDetector;

@Environment(EnvType.CLIENT)
@Mixin(ChatComponent.class)
public class ChatHudMixin {

    @Shadow
    @Final
    @Mutable
    private Minecraft minecraft;

    /** Tracks the channel of the most recent non-continuation message. */
    private static ChatType currentChatType = ChatType.NORMAL;

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD")
    )
    private void wynnbubbles$onAddMessage(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag indicator, CallbackInfo ci) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;

        String rawMessage = message.getString();
        boolean debug = WynnBubbles.CONFIG.debugMode;

        if (debug) WynnBubbles.LOGGER.info("[WynnBubbles] addMessage: '{}'",
                rawMessage.substring(0, Math.min(80, rawMessage.length())).replace("\n", "\\n"));

        currentChatType = WynnChatDetector.detectChatType(rawMessage, currentChatType);
        if (debug) WynnBubbles.LOGGER.info("[WynnBubbles] chatType={}", currentChatType);

        String senderName = extractSender(message);
        if (debug) WynnBubbles.LOGGER.info("[WynnBubbles] senderName='{}'", senderName);
        if (senderName.isEmpty()) {
            if (debug) WynnBubbles.LOGGER.info("[WynnBubbles] senderName empty, skipping");
            return;
        }

        UUID senderUUID = minecraft.getPlayerSocialManager().getDiscoveredUUID(senderName);
        if (debug) WynnBubbles.LOGGER.info("[WynnBubbles] senderUUID={}", senderUUID);

        List<AbstractClientPlayer> nearby = minecraft.level.getEntitiesOfClass(
                AbstractClientPlayer.class,
                minecraft.player.getBoundingBox().inflate(WynnBubbles.CONFIG.chatRange),
                EntitySelector.NO_SPECTATORS
        );
        if (debug) WynnBubbles.LOGGER.info("[WynnBubbles] nearby players={}", nearby.size());

        if (!WynnBubbles.CONFIG.showOwnBubble) {
            nearby.remove(minecraft.player);
        }

        for (AbstractClientPlayer player : nearby) {
            if (debug) WynnBubbles.LOGGER.info("[WynnBubbles] checking player {} uuid={}", player.getName().getString(), player.getUUID());
            if (!player.getUUID().equals(senderUUID)) continue;

            // Extract the message body (everything after "SenderName: ")
            int namePos = rawMessage.indexOf(senderName);
            if (namePos == -1) { if (debug) WynnBubbles.LOGGER.info("[WynnBubbles] namePos not found"); break; }
            int colonPos = rawMessage.indexOf(":", namePos) + 1;
            if (colonPos == 0) { if (debug) WynnBubbles.LOGGER.info("[WynnBubbles] colon not found"); break; }

            String body = WynnChatDetector.removeUnicodeSequences(rawMessage.substring(colonPos).trim());
            if (debug) WynnBubbles.LOGGER.info("[WynnBubbles] body='{}'", body);
            if (body.isEmpty()) { if (debug) WynnBubbles.LOGGER.info("[WynnBubbles] body empty after cleaning"); break; }

            ((AbstractClientPlayerEntityAccessor) player).wynnbubbles$addBubbleMessage(
                    new BubbleMessage(Component.literal(body), currentChatType, player.tickCount)
            );
            if (debug) WynnBubbles.LOGGER.info("[WynnBubbles] bubble added to {}", player.getName().getString());
            break;
        }
    }

    private String extractSender(Component text) {
        String[] words = text.getString().split("(§.)|[^\\w§]+");
        String[] parts = text.toString().split("key='");

        if (parts.length > 1) {
            String translationKey = parts[1].split("'")[0];
            if (translationKey.contains("commands") || translationKey.contains("advancement")) {
                return "";
            }
        }

        for (int i = 0; i < words.length; i++) {
            if (words[i].isEmpty()) continue;
            if (WynnBubbles.CONFIG.maxUUIDWordCheck != 0 && i >= WynnBubbles.CONFIG.maxUUIDWordCheck) return "";
            UUID possibleUUID = minecraft.getPlayerSocialManager().getDiscoveredUUID(words[i]);
            if (possibleUUID != Util.NIL_UUID) return words[i];
        }

        return "";
    }
}
