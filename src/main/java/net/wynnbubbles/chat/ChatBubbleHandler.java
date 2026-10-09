package net.wynnbubbles.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.wynnbubbles.WynnBubbles;
import net.wynnbubbles.bubble.Bubble;
import net.wynnbubbles.chat.ChatParser.Nickname;
import net.wynnbubbles.chat.ChatParser.ParsedChat;
import net.wynnbubbles.identity.Identities;
import net.wynnbubbles.identity.SenderMatcher;
import net.wynnbubbles.util.ChatType;
import net.wynnbubbles.util.WynnChatDetector;

public final class ChatBubbleHandler {
    /** Channel of the last non-continuation line; Wynncraft's wrapped lines inherit it. */
    private static ChatType currentChatType = ChatType.NORMAL;

    public static void onChatMessage(Component message) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (mc.player == null || mc.level == null || connection == null) return;
        if (isSystemMessage(message)) return;

        String raw = message.getString();
        currentChatType = WynnChatDetector.detectChatType(raw, currentChatType);

        List<AbstractClientPlayer> players = mc.level.players();
        Predicate<String> isPlayerName = word -> connection.getPlayerInfo(word) != null
                || players.stream().anyMatch(p -> p.getGameProfile().name().equalsIgnoreCase(word));

        List<Nickname> nicknames = findNicknames(message);
        Optional<ParsedChat> parsed = ChatParser.parse(
                raw, currentChatType, isPlayerName, WynnBubbles.CONFIG.maxUUIDWordCheck, nicknames);
        if (parsed.isEmpty()) return;
        ParsedChat chat = parsed.get();

        // The tab list is where Wynntils reads account UUIDs from, ghosts included
        PlayerInfo info = connection.getPlayerInfo(chat.sender());
        UUID tabListId = info == null ? null : info.getProfile().id();

        double rangeSq = WynnBubbles.CONFIG.chatRange * WynnBubbles.CONFIG.chatRange;
        List<AbstractClientPlayer> candidates = players.stream()
                .filter(p -> !p.isSpectator() && !Identities.isNpc(p))
                .filter(p -> p != mc.player || WynnBubbles.CONFIG.showOwnBubble)
                .filter(p -> p.distanceToSqr(mc.player) <= rangeSq)
                .toList();

        Optional<AbstractClientPlayer> sender = SenderMatcher.find(
                chat.sender(), tabListId, candidates, p -> p.getGameProfile().name(), Identities::canonicalId);

        if (WynnBubbles.CONFIG.debugMode) {
            WynnBubbles.LOGGER.info(
                    "[WynnBubbles] {} from '{}' (nicknames {}, tab list id {}, wynntils {}): {}",
                    chat.type(),
                    chat.sender(),
                    nicknames,
                    tabListId,
                    Identities.usingWynntils(),
                    sender.map(p -> "matched entity " + p.getUUID() + (Identities.isGhost(p) ? " (ghost)" : ""))
                            .orElse("no matching player among " + candidates.size() + " in range"));
        }

        sender.ifPresent(player -> WynnBubbles.BUBBLES.add(
                Identities.canonicalId(player),
                new Bubble(Component.literal(chat.body()), chat.type(), WynnBubbles.clientTick()),
                WynnBubbles.CONFIG.maxBubbles));
    }

    /** Champion+ players chat under a nickname; the real username is only in the name's hover text. */
    private static List<Nickname> findNicknames(Component message) {
        List<Nickname> nicknames = new ArrayList<>();
        message.visit(
                (style, text) -> {
                    if (style.getHoverEvent() instanceof HoverEvent.ShowText(Component hover)) {
                        ChatParser.parseNicknameHover(hover.getString())
                                .filter(nickname -> !nicknames.contains(nickname))
                                .ifPresent(nicknames::add);
                    }
                    return Optional.empty();
                },
                Style.EMPTY);
        return nicknames;
    }

    private static boolean isSystemMessage(Component message) {
        if (!(message.getContents() instanceof TranslatableContents translatable)) return false;
        String key = translatable.getKey();
        return key.contains("commands") || key.contains("advancement");
    }

    private ChatBubbleHandler() {}
}
