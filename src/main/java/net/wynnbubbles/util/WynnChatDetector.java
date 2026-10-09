package net.wynnbubbles.util;

/**
 * Wynncraft-specific chat channel detection constants and helpers.
 * Wynncraft embeds private-use Unicode sequences at the start of every chat message
 * to identify the channel it was sent in.
 */
public final class WynnChatDetector {

    public static final String PARTY_CREATION_MESSAGE = "You have successfully created a party";

    public static final int[][] GUILD_SEQUENCES = {
            {0xDAFF, 0xDFFC, 0xE006, 0xDAFF, 0xDFFF, 0xE002, 0xDAFF, 0xDFFE},
    };
    public static final int[][] PARTY_SEQUENCES = {
            {0xDAFF, 0xDFFC, 0xE005, 0xDAFF, 0xDFFF, 0xE002, 0xDAFF, 0xDFFE},
    };
    public static final int[][] PRIVATE_MESSAGE_SEQUENCES = {
            {0xDAFF, 0xDFFC, 0xE007, 0xDAFF, 0xDFFF, 0xE002, 0xDAFF, 0xDFFE}
    };

    public static final int[] CONTINUATION_SEQUENCE = {
            0xDAFF, 0xDFFC, 0xE001, 0xDB00, 0xDC06
    };
    public static final int[] NEWLINE_CONTINUATION_SEQUENCE = {
            0x000A, 0xDAFF, 0xDFFC, 0xE001, 0xDB00, 0xDC06
    };

    /** Returns true if {@code text} starts with {@code sequence} at position 0. */
    public static boolean matchesSequence(String text, int[] sequence) {
        if (text == null || text.length() < sequence.length) return false;
        for (int i = 0; i < sequence.length; i++) {
            if (text.charAt(i) != sequence[i]) return false;
        }
        return true;
    }

    /** Returns true if {@code text} starts with {@code sequence} at the given offset. */
    public static boolean matchesSequence(String text, int startIndex, int[] sequence) {
        if (text == null || startIndex + sequence.length > text.length()) return false;
        for (int i = 0; i < sequence.length; i++) {
            if (text.charAt(startIndex + i) != sequence[i]) return false;
        }
        return true;
    }

    /** Returns true if {@code text} starts with any of the given sequences. */
    public static boolean matchesAnySequence(String text, int[][] sequences) {
        for (int[] seq : sequences) {
            if (matchesSequence(text, seq)) return true;
        }
        return false;
    }

    /**
     * Removes Wynncraft's continuation Unicode sequences from a string,
     * replacing them with a space (avoiding double spaces).
     */
    public static String removeUnicodeSequences(String text) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            if (matchesSequence(text, i, NEWLINE_CONTINUATION_SEQUENCE)) {
                i += NEWLINE_CONTINUATION_SEQUENCE.length;
                if (out.length() > 0 && out.charAt(out.length() - 1) != ' ') out.append(' ');
            } else if (matchesSequence(text, i, CONTINUATION_SEQUENCE)) {
                i += CONTINUATION_SEQUENCE.length;
                if (out.length() > 0 && out.charAt(out.length() - 1) != ' ') out.append(' ');
            } else {
                out.append(text.charAt(i));
                i++;
            }
        }
        return out.toString().trim();
    }

    /** Determines the {@link ChatType} for an incoming raw Wynncraft chat message string. */
    public static ChatType detectChatType(String rawText, ChatType currentType) {
        if (rawText.contains(PARTY_CREATION_MESSAGE)) return ChatType.PARTY;

        if (matchesSequence(rawText, 0, CONTINUATION_SEQUENCE) ||
                matchesSequence(rawText, 0, NEWLINE_CONTINUATION_SEQUENCE)) {
            return currentType; // continuation of previous message, same channel
        }

        if (matchesAnySequence(rawText, PRIVATE_MESSAGE_SEQUENCES)) return ChatType.PRIVATE;
        if (matchesAnySequence(rawText, GUILD_SEQUENCES))            return ChatType.GUILD;
        if (matchesAnySequence(rawText, PARTY_SEQUENCES))            return ChatType.PARTY;

        return ChatType.NORMAL;
    }

    private WynnChatDetector() {}
}
