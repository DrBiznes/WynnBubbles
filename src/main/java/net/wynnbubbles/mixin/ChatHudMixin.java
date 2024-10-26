package net.wynnbubbles.mixin;

import java.util.ArrayList;
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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.wynnbubbles.WynnBubbles;
import net.wynnbubbles.accessor.AbstractClientPlayerEntityAccessor;

@Environment(EnvType.CLIENT)
@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Shadow
    @Final
    @Mutable
    private MinecraftClient client;

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"))
    private void addMessageMixin(Text message, @Nullable MessageSignatureData signature, @Nullable MessageIndicator indicator, CallbackInfo info) {
        if (client != null && client.player != null) {
            String rawMessage = message.getString(); // Get the raw message with Unicode characters
            System.out.println("Debug - Raw message received: " + rawMessage); // Debug print

            String detectedSenderName = extractSender(message);
            if (!detectedSenderName.isEmpty()) {
                UUID senderUUID = this.client.getSocialInteractionsManager().getUuid(detectedSenderName);

                List<AbstractClientPlayerEntity> list = client.world.getEntitiesByClass(AbstractClientPlayerEntity.class,
                        client.player.getBoundingBox().expand(WynnBubbles.CONFIG.chatRange),
                        EntityPredicates.EXCEPT_SPECTATOR);

                if (!WynnBubbles.CONFIG.showOwnBubble) {
                    list.remove(client.player);
                }

                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getUuid().equals(senderUUID)) {
                        // Split the message while preserving Unicode characters
                        String[] words = rawMessage.split(" ");
                        List<String> stringList = new ArrayList<>();
                        String stringCollector = "";

                        int width = 0;
                        int height = 0;

                        for (String word : words) {
                            if (client.textRenderer.getWidth(stringCollector) < WynnBubbles.CONFIG.maxChatWidth
                                    && client.textRenderer.getWidth(stringCollector + " " + word) <= WynnBubbles.CONFIG.maxChatWidth) {
                                stringCollector = stringCollector.isEmpty() ? word : stringCollector + " " + word;
                            } else {
                                if (!stringCollector.isEmpty()) {
                                    stringList.add(stringCollector);
                                    height++;
                                    width = Math.max(width, client.textRenderer.getWidth(stringCollector));
                                }
                                stringCollector = word;
                            }
                        }

                        if (!stringCollector.isEmpty()) {
                            stringList.add(stringCollector);
                            height++;
                            width = Math.max(width, client.textRenderer.getWidth(stringCollector));
                        }

                        if (width % 2 != 0) {
                            width++;
                        }

                        ((AbstractClientPlayerEntityAccessor) list.get(i)).setChatText(stringList, list.get(i).age, width, height);
                        break;
                    }
                }
            }
        }
    }

    private String extractSender(Text text) {
        String[] words = text.getString().split("(§.)|[^\\w§]+");
        String[] parts = text.toString().split("key='");

        if (parts.length > 1) {
            String translationKey = parts[1].split("'")[0];
            if (translationKey.contains("commands")) {
                return "";
            } else if (translationKey.contains("advancement")) {
                return "";
            }
        }

        for (int i = 0; i < words.length; i++) {
            if (words[i].isEmpty()) {
                continue;
            }
            if (WynnBubbles.CONFIG.maxUUIDWordCheck != 0 && i >= WynnBubbles.CONFIG.maxUUIDWordCheck) {
                return "";
            }

            UUID possibleUUID = this.client.getSocialInteractionsManager().getUuid(words[i]);
            if (possibleUUID != Util.NIL_UUID) {
                return words[i];
            }
        }

        return "";
    }
}