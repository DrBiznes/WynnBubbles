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
import net.wynnbubbles.util.RenderBubble;
import net.wynnbubbles.util.RenderBubble.ChatType;

@Environment(EnvType.CLIENT)
@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Shadow
    @Final
    @Mutable
    private MinecraftClient client;

    private static final int[] CONTINUATION_SEQUENCE = {
            0xDAFF, 0xDFFC, 0xE001, 0xDB00, 0xDC06
    };

    private static final int[] NEWLINE_CONTINUATION_SEQUENCE = {
            0x000A, 0xDAFF, 0xDFFC, 0xE001, 0xDB00, 0xDC06
    };

    // Keep track of current chat type state
    private static ChatType currentChatType = ChatType.NORMAL;

    private static boolean matchesSequence(String text, int startIndex, int[] sequence) {
        if (text == null || text.isEmpty() || startIndex + sequence.length > text.length()) return false;

        for (int i = 0; i < sequence.length; i++) {
            if (text.charAt(startIndex + i) != sequence[i]) return false;
        }
        return true;
    }

    private static String removeAllUnicodeSequences(String text) {
        StringBuilder cleanedText = new StringBuilder();
        int i = 0;

        // Then process the text
        while (i < text.length()) {
            // Check for newline continuation sequence
            if (matchesSequence(text, i, NEWLINE_CONTINUATION_SEQUENCE)) {
                i += NEWLINE_CONTINUATION_SEQUENCE.length;
                if (cleanedText.length() > 0 && cleanedText.charAt(cleanedText.length() - 1) != ' ') {
                    cleanedText.append(' ');
                }
            }
            // Check for regular continuation sequence
            else if (matchesSequence(text, i, CONTINUATION_SEQUENCE)) {
                i += CONTINUATION_SEQUENCE.length;
                if (cleanedText.length() > 0 && cleanedText.charAt(cleanedText.length() - 1) != ' ') {
                    cleanedText.append(' ');
                }
            }
            // Normal character
            else {
                cleanedText.append(text.charAt(i));
                i++;
            }
        }

        return cleanedText.toString().trim();
    }

    private ChatType determineChatType(String text) {
        System.out.println("ChatHud: Analyzing message: " + text);

        // Debug print the first few characters
        System.out.print("First characters (hex): ");
        for (int i = 0; i < Math.min(text.length(), 20); i++) {
            System.out.printf("0x%04X ", (int)text.charAt(i));
        }
        System.out.println();

        // Check for party creation message
        if (text.contains(RenderBubble.PARTY_CREATION_MESSAGE)) {
            System.out.println("ChatHud: Party creation message detected - updating chat type");
            currentChatType = ChatType.PARTY;
            return currentChatType;
        }

        // Check for continuation sequence first - if found, maintain current type
        if (matchesSequence(text, 0, CONTINUATION_SEQUENCE) ||
                matchesSequence(text, 0, NEWLINE_CONTINUATION_SEQUENCE)) {
            System.out.println("ChatHud: Continuation sequence detected - maintaining type: " + currentChatType);
            return currentChatType;
        }

        // Check for private message sequence
        if (RenderBubble.matchesAnySequence(text, RenderBubble.PRIVATE_MESSAGE_SEQUENCES)) {
            System.out.println("ChatHud: Private message sequence detected");
            currentChatType = ChatType.PRIVATE;
            return ChatType.PRIVATE;
        }

        // Check for guild chat sequence
        if (RenderBubble.matchesAnySequence(text, RenderBubble.GUILD_SEQUENCES)) {
            System.out.println("ChatHud: Guild chat sequence detected");
            currentChatType = ChatType.GUILD;
            return ChatType.GUILD;
        }

        // Check for party chat sequence
        if (RenderBubble.matchesAnySequence(text, RenderBubble.PARTY_SEQUENCES)) {
            System.out.println("ChatHud: Party chat sequence detected");
            currentChatType = ChatType.PARTY;
            return ChatType.PARTY;
        }

        // If we get here and don't find any special sequences, it's a new normal message
        currentChatType = ChatType.NORMAL;
        System.out.println("ChatHud: Normal chat detected");
        return ChatType.NORMAL;
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"))
    private void addMessageMixin(Text message, @Nullable MessageSignatureData signature, @Nullable MessageIndicator indicator, CallbackInfo info) {
        if (client != null && client.player != null) {
            String rawMessage = message.getString();
            System.out.println("ChatHud: Raw message received: " + rawMessage);

            // Determine chat type for this message
            ChatType chatType = determineChatType(rawMessage);

            // Skip creating bubbles for system messages by checking for player sender
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

                                // Clean the message of Unicode sequences
                                actualMessage = removeAllUnicodeSequences(actualMessage);

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

                                List<String> finalList = new ArrayList<>();
                                finalList.add(rawMessage); // First line contains full raw message
                                finalList.addAll(stringList);

                                ((AbstractClientPlayerEntityAccessor) list.get(i)).setChatText(finalList, list.get(i).age, width, height, chatType);
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