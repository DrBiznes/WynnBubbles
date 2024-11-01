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

        while (i < text.length()) {
            if (matchesSequence(text, i, NEWLINE_CONTINUATION_SEQUENCE)) {
                i += NEWLINE_CONTINUATION_SEQUENCE.length;
                if (cleanedText.length() > 0 && cleanedText.charAt(cleanedText.length() - 1) != ' ') {
                    cleanedText.append(' ');
                }
            }
            else if (matchesSequence(text, i, CONTINUATION_SEQUENCE)) {
                i += CONTINUATION_SEQUENCE.length;
                if (cleanedText.length() > 0 && cleanedText.charAt(cleanedText.length() - 1) != ' ') {
                    cleanedText.append(' ');
                }
            }
            else {
                cleanedText.append(text.charAt(i));
                i++;
            }
        }

        return cleanedText.toString().trim();
    }

    private ChatType determineChatType(String text) {
        if (text.contains(RenderBubble.PARTY_CREATION_MESSAGE)) {
            currentChatType = ChatType.PARTY;
            return currentChatType;
        }

        if (matchesSequence(text, 0, CONTINUATION_SEQUENCE) ||
                matchesSequence(text, 0, NEWLINE_CONTINUATION_SEQUENCE)) {
            return currentChatType;
        }

        if (RenderBubble.matchesAnySequence(text, RenderBubble.PRIVATE_MESSAGE_SEQUENCES)) {
            currentChatType = ChatType.PRIVATE;
            return ChatType.PRIVATE;
        }

        if (RenderBubble.matchesAnySequence(text, RenderBubble.GUILD_SEQUENCES)) {
            currentChatType = ChatType.GUILD;
            return ChatType.GUILD;
        }

        if (RenderBubble.matchesAnySequence(text, RenderBubble.PARTY_SEQUENCES)) {
            currentChatType = ChatType.PARTY;
            return ChatType.PARTY;
        }

        currentChatType = ChatType.NORMAL;
        return ChatType.NORMAL;
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"))
    private void addMessageMixin(Text message, @Nullable MessageSignatureData signature, @Nullable MessageIndicator indicator, CallbackInfo info) {
        if (client != null && client.player != null) {
            String rawMessage = message.getString();
            ChatType chatType = determineChatType(rawMessage);

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
                        int messageStart = rawMessage.indexOf(detectedSenderName);
                        if (messageStart != -1) {
                            messageStart = rawMessage.indexOf(":", messageStart) + 1;
                            if (messageStart != 0) {
                                String actualMessage = rawMessage.substring(messageStart).trim();
                                actualMessage = removeAllUnicodeSequences(actualMessage);

                                String[] words = actualMessage.split(" ");
                                List<String> stringList = new ArrayList<>();
                                String stringCollector = "";

                                int width = 0;
                                int height = 0;

                                if (words.length > 0) {
                                    stringCollector = words[0];
                                }

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

                                if (!stringCollector.isEmpty()) {
                                    stringList.add(stringCollector);
                                    height++;
                                    width = Math.max(width, client.textRenderer.getWidth(stringCollector));
                                }

                                if (width % 2 != 0) {
                                    width++;
                                }

                                List<String> finalList = new ArrayList<>();
                                finalList.add(rawMessage);
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
            if (translationKey.contains("commands") || translationKey.contains("advancement")) {
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