package net.wynnbubbles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.wynnbubbles.chat.ChatParser;
import net.wynnbubbles.chat.ChatParser.ParsedChat;
import net.wynnbubbles.util.ChatType;
import net.wynnbubbles.util.WynnChatDetector;
import org.junit.jupiter.api.Test;

class ChatParsingTest {
    private static final String GUILD = "󏿼󏿿󏿾";
    private static final String PARTY = "󏿼󏿿󏿾";
    private static final String PRIVATE = "󏿼󏿿󏿾";
    private static final String CONTINUATION = "󏿼󐀆";

    private static final Predicate<String> KNOWN = Set.of("Alice", "Bob")::contains;

    private static Optional<ParsedChat> parse(String raw) {
        return ChatParser.parse(raw, WynnChatDetector.detectChatType(raw, ChatType.NORMAL), KNOWN, 0);
    }

    @Test
    void channelsAreDetectedFromTheirPrefix() {
        assertEquals(ChatType.NORMAL, WynnChatDetector.detectChatType("Alice: hi", ChatType.NORMAL));
        assertEquals(ChatType.GUILD, WynnChatDetector.detectChatType(GUILD + " Alice: hi", ChatType.NORMAL));
        assertEquals(ChatType.PARTY, WynnChatDetector.detectChatType(PARTY + " Alice: hi", ChatType.NORMAL));
        assertEquals(ChatType.PRIVATE, WynnChatDetector.detectChatType(PRIVATE + " Alice: hi", ChatType.NORMAL));
    }

    @Test
    void aNormalLineResetsTheChannel() {
        assertEquals(ChatType.NORMAL, WynnChatDetector.detectChatType("Alice: hi", ChatType.GUILD));
    }

    @Test
    void continuationLinesInheritTheChannel() {
        assertEquals(ChatType.GUILD, WynnChatDetector.detectChatType(CONTINUATION + " more text", ChatType.GUILD));
        assertEquals(
                ChatType.PARTY, WynnChatDetector.detectChatType("\n" + CONTINUATION + " more text", ChatType.PARTY));
    }

    @Test
    void senderAndBodyAreSplitAtTheColon() {
        ParsedChat chat = parse("Alice: hello there").orElseThrow();

        assertEquals("Alice", chat.sender());
        assertEquals("hello there", chat.body());
        assertEquals(ChatType.NORMAL, chat.type());
    }

    @Test
    void guildLineKeepsItsChannelAndDropsThePrefix() {
        ParsedChat chat = parse(GUILD + " Bob: guild meeting at 8").orElseThrow();

        assertEquals("Bob", chat.sender());
        assertEquals("guild meeting at 8", chat.body());
        assertEquals(ChatType.GUILD, chat.type());
    }

    @Test
    void colonsInsideTheMessageAreKept() {
        assertEquals("meet at 8:30", parse("Alice: meet at 8:30").orElseThrow().body());
    }

    @Test
    void wrapMarkersInsideTheMessageBecomeSpaces() {
        ParsedChat chat = parse(PARTY + " Alice: first half\n" + CONTINUATION + "second half").orElseThrow();

        assertEquals("first half second half", chat.body());
    }

    @Test
    void privateMessageBelongsToTheFirstKnownName() {
        assertEquals("Bob", parse(PRIVATE + " Bob  Alice: psst").orElseThrow().sender());
    }

    @Test
    void linesWithoutAKnownPlayerAreIgnored() {
        assertTrue(parse("Carol: hi").isEmpty());
        assertTrue(parse("[!] The raid begins in 10 seconds: get ready").isEmpty());
    }

    @Test
    void linesThatOnlyMentionAPlayerAreIgnored() {
        assertTrue(parse("Alice has logged in").isEmpty());
    }

    @Test
    void emptyMessagesAreIgnored() {
        assertTrue(parse("Alice:   ").isEmpty());
    }

    @Test
    void wordLimitStopsTheSearchForASender() {
        String raw = "one two three Alice: hi";

        assertTrue(ChatParser.parse(raw, ChatType.NORMAL, KNOWN, 3).isEmpty());
        assertEquals("Alice", ChatParser.parse(raw, ChatType.NORMAL, KNOWN, 4).orElseThrow().sender());
    }
}
