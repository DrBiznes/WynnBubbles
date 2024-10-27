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

    private static final int[] CONTINUATION_SEQUENCE = {
            0xDAFF, 0xDFFC, 0xE001, 0xDB00, 0xDC06
    };

    private static final int[] NEWLINE_CONTINUATION_SEQUENCE = {
            0x000A, 0xDAFF, 0xDFFC, 0xE001, 0xDB00, 0xDC06
    };

    private static final int[][] INITIAL_SEQUENCES = {
            // Party chat initial sequence
            {0xDAFF, 0xDFFC, 0xE005, 0xDAFF, 0xDFFF, 0xE002, 0xDAFF, 0xDFFE},
            // Guild chat initial sequence
            {0xDAFF, 0xDFFC, 0xE006, 0xDAFF, 0xDFFF, 0xE002, 0xDAFF, 0xDFFE}
    };

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

        // First check for initial sequences at the start
        for (int[] sequence : INITIAL_SEQUENCES) {
            if (matchesSequence(text, 0, sequence)) {
                i = sequence.length;
                break;
            }
        }

        // Then process the rest of the text
        while (i < text.length()) {
            // Check for newline continuation sequence
            if (matchesSequence(text, i, NEWLINE_CONTINUATION_SEQUENCE)) {
                // Skip the sequence but add a space
                i += NEWLINE_CONTINUATION_SEQUENCE.length;
                if (cleanedText.length() > 0 && cleanedText.charAt(cleanedText.length() - 1) != ' ') {
                    cleanedText.append(' ');
                }
            }
            // Check for regular continuation sequence
            else if (matchesSequence(text, i, CONTINUATION_SEQUENCE)) {
                // Skip the sequence but add a space
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

                                // Remove all Unicode sequences from the actual message
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