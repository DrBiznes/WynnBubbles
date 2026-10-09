package net.wynnbubbles.chat;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.wynnbubbles.util.ChatType;
import net.wynnbubbles.util.WynnChatDetector;

public final class ChatParser {
    /** Wynncraft's tooltip on a nicknamed sender */
    private static final Pattern REAL_NAME_HOVER =
            Pattern.compile("^(?<nick>.+?)'s? real (?:user)?name is (?<username>\\w{1,16})", Pattern.MULTILINE);
    /** What Wynntils' "Reveal Nicknames" feature rewrites that tooltip to */
    private static final Pattern NICKNAME_HOVER =
            Pattern.compile("^(?<username>\\w{1,16})'s nickname is (?<nick>.+)$", Pattern.MULTILINE);

    public record ParsedChat(ChatType type, String sender, String body) {}

    /** A chat display name and the account username it stands for. */
    public record Nickname(String nick, String username) {}

    /** Reads the nickname out of the hover text of a chat name, if that is what the hover is. */
    public static Optional<Nickname> parseNicknameHover(String hoverText) {
        String text = hoverText.replaceAll("§.", "");
        for (Pattern pattern : List.of(REAL_NAME_HOVER, NICKNAME_HOVER)) {
            Matcher matcher = pattern.matcher(text);
            if (!matcher.find()) continue;
            String nick = matcher.group("nick").trim();
            if (!nick.isEmpty()) return Optional.of(new Nickname(nick, matcher.group("username")));
        }
        return Optional.empty();
    }

    /**
     * @param isPlayerName whether a word is the name of a known player
     * @param maxWords     how many leading words to try as the sender, 0 for all
     */
    public static Optional<ParsedChat> parse(String raw, ChatType type, Predicate<String> isPlayerName, int maxWords) {
        return parse(raw, type, isPlayerName, maxWords, List.of());
    }

    /**
     * @param isPlayerName whether a word is the name of a known player
     * @param maxWords     how many leading words to try as the sender, 0 for all
     * @param nicknames    nicknames found in the line's hover texts
     */
    public static Optional<ParsedChat> parse(
            String raw, ChatType type, Predicate<String> isPlayerName, int maxWords, List<Nickname> nicknames) {
        String sender = findSender(raw, isPlayerName, maxWords);
        int senderAt = sender == null ? -1 : raw.indexOf(sender);

        // Whoever is named first is the sender; a nickname wins a tie since it may look like someone else's name
        for (Nickname nickname : nicknames) {
            int nickAt = raw.indexOf(nickname.nick());
            if (nickAt != -1 && (senderAt == -1 || nickAt <= senderAt)) {
                sender = nickname.username();
                senderAt = nickAt;
            }
        }
        if (sender == null) return Optional.empty();

        int colon = raw.indexOf(':', senderAt);
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
