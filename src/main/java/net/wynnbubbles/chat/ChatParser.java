package net.wynnbubbles.chat;

import java.util.Optional;
import java.util.function.Predicate;
import net.wynnbubbles.util.ChatType;
import net.wynnbubbles.util.WynnChatDetector;

public final class ChatParser {

    public record ParsedChat(ChatType type, String sender, String body) {}

    /**
     * @param isPlayerName whether a word is the name of a known player
     * @param maxWords     how many leading words to try as the sender, 0 for all
     */
    public static Optional<ParsedChat> parse(String raw, ChatType type, Predicate<String> isPlayerName, int maxWords) {
        String sender = findSender(raw, isPlayerName, maxWords);
        if (sender == null) return Optional.empty();

        int colon = raw.indexOf(':', raw.indexOf(sender));
        if (colon == -1) return Optional.empty();

        String body = WynnChatDetector.removeUnicodeSequences(raw.substring(colon + 1).trim());
        if (body.isEmpty()) return Optional.empty();

        return Optional.of(new ParsedChat(type, sender, body));
    }

    private static String findSender(String raw, Predicate<String> isPlayerName, int maxWords) {
        String[] words = raw.split("(§.)|[^\\w§]+");
        for (int i = 0; i < words.length; i++) {
            if (words[i].isEmpty()) continue;
            if (maxWords != 0 && i >= maxWords) return null;
            if (isPlayerName.test(words[i])) return words[i];
        }
        return null;
    }

    private ChatParser() {}
}
