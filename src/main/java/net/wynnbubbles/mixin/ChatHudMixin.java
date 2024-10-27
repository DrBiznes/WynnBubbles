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
            String rawMessage = message.getString(); // Get the raw message for Unicode detection
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
                        // Extract just the actual message part after the player name
                        int messageStart = rawMessage.indexOf(detectedSenderName);
                        if (messageStart != -1) {
                            messageStart = rawMessage.indexOf(":", messageStart) + 1;
                            if (messageStart != 0) { // Found the colon
                                String actualMessage = rawMessage.substring(messageStart).trim();

                                // Split the message into lines for the bubble
                                String[] words = actualMessage.split(" ");
                                List<String> stringList = new ArrayList<>();
                                String stringCollector = "";

                                int width = 0;
                                int height = 0;

                                // Handle the first word separately to avoid leading space
                                if (words.length > 0) {
                                    stringCollector = words[0];
                                }

                                // Process remaining words
                                for (int j = 1; j < words.length; j++) {
                                    String word = words[j];
                                    String potentialLine = stringCollector + " " + word;

                                    if (client.textRenderer.getWidth(potentialLine) <= WynnBubbles.CONFIG.maxChatWidth) {
                                        stringCollector = potentialLine;
                                    } else {
                                        stringList.add(stringCollector);
                                        height++;
                                        width = Math.max(width, client.textRenderer.getWidth(stringCollector));
                                        stringCollector = word;
                                    }
                                }

                                // Add the last line
                                if (!stringCollector.isEmpty()) {
                                    stringList.add(stringCollector);
                                    height++;
                                    width = Math.max(width, client.textRenderer.getWidth(stringCollector));
                                }

                                if (width % 2 != 0) {
                                    width++;
                                }

                                // Pass the full raw message as the first line for color detection,
                                // followed by the actual message lines
                                List<String> finalList = new ArrayList<>();
                                finalList.add(rawMessage); // First line contains full message with Unicode for color detection
                                finalList.addAll(stringList); // Subsequent lines contain actual visible message

                                ((AbstractClientPlayerEntityAccessor) list.get(i)).setChatText(finalList, list.get(i).age, width, height);
                            }
                        }
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