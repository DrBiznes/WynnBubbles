package net.wynnbubbles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.wynnbubbles.bubble.Bubble;
import net.wynnbubbles.bubble.BubbleStore;
import net.wynnbubbles.client.BubbleLayout;
import net.wynnbubbles.client.BubbleLayout.Box;
import net.wynnbubbles.client.BubbleLayout.TextSize;
import net.wynnbubbles.identity.PlayerIds;
import net.wynnbubbles.util.ChatType;
import org.junit.jupiter.api.Test;

class BubbleStackingTest {
    private static final UUID ALICE = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID ALICE_GHOST = UUID.fromString("11111111-1111-2111-8111-111111111111");
    private static final UUID BOB = UUID.fromString("22222222-2222-4222-8222-222222222222");

    private static Bubble bubble(String text, long tick) {
        return new Bubble(Component.literal(text), ChatType.NORMAL, tick);
    }

    private static List<String> texts(BubbleStore store, UUID id) {
        return store.get(id).stream().map(b -> b.text().getString()).toList();
    }

    @Test
    void newestBubbleComesFirst() {
        BubbleStore store = new BubbleStore();
        store.add(ALICE, bubble("one", 0), 5);
        store.add(ALICE, bubble("two", 1), 5);
        store.add(ALICE, bubble("three", 2), 5);

        assertEquals(List.of("three", "two", "one"), texts(store, ALICE));
    }

    @Test
    void oldestBubbleIsDroppedPastTheLimit() {
        BubbleStore store = new BubbleStore();
        for (int i = 1; i <= 4; i++) store.add(ALICE, bubble("msg" + i, i), 3);

        assertEquals(List.of("msg4", "msg3", "msg2"), texts(store, ALICE));
    }

    @Test
    void theSameLineDeliveredSeveralTimesMakesOneBubble() {
        BubbleStore store = new BubbleStore();
        store.add(ALICE, bubble("yo", 10), 5);
        store.add(ALICE, bubble("yo", 10), 5);
        store.add(ALICE, bubble("yo", 11), 5);

        assertEquals(List.of("yo"), texts(store, ALICE));
    }

    @Test
    void sayingTheSameThingAgainLaterMakesANewBubble() {
        BubbleStore store = new BubbleStore();
        store.add(ALICE, bubble("yo", 10), 5);
        store.add(ALICE, bubble("yo", 40), 5);

        assertEquals(List.of("yo", "yo"), texts(store, ALICE));
    }

    @Test
    void playersDoNotShareBubbles() {
        BubbleStore store = new BubbleStore();
        store.add(ALICE, bubble("from alice", 0), 5);
        store.add(BOB, bubble("from bob", 0), 5);

        assertEquals(List.of("from alice"), texts(store, ALICE));
        assertEquals(List.of("from bob"), texts(store, BOB));
    }

    @Test
    void eachBubbleExpiresOnItsOwnTimer() {
        BubbleStore store = new BubbleStore();
        store.add(ALICE, bubble("old", 0), 5);
        store.add(ALICE, bubble("new", 50), 5);

        store.expire(100, 100);
        assertEquals(List.of("new", "old"), texts(store, ALICE));

        store.expire(101, 100);
        assertEquals(List.of("new"), texts(store, ALICE));

        store.expire(151, 100);
        assertTrue(store.get(ALICE).isEmpty());
    }

    @Test
    void ghostAndRealPlayerShareOneStack() {
        BubbleStore store = new BubbleStore();
        store.add(PlayerIds.canonical(ALICE), bubble("as a real player", 0), 5);
        store.add(PlayerIds.canonical(ALICE_GHOST), bubble("as a ghost", 1), 5);

        assertEquals(List.of("as a ghost", "as a real player"), texts(store, PlayerIds.canonical(ALICE_GHOST)));
    }

    @Test
    void bubblesStackUpwardsWithoutOverlapping() {
        List<Box> boxes = BubbleLayout.stack(
                List.of(new TextSize(40, 1), new TextSize(170, 3), new TextSize(12, 1)), 0, 2);

        // y grows downwards: everything sits above the anchor, and the arrow fits under the newest bubble
        assertEquals(-(BubbleLayout.ARROW_HEIGHT - 1), boxes.get(0).bottom());
        for (int i = 1; i < boxes.size(); i++) {
            assertEquals(boxes.get(i - 1).y() - 2, boxes.get(i).bottom(), "gap below bubble " + i);
        }
    }

    @Test
    void bubblesAreCentredAndFitTheirText() {
        for (TextSize text : List.of(new TextSize(1, 1), new TextSize(40, 1), new TextSize(41, 2), new TextSize(180, 4))) {
            Box box = BubbleLayout.stack(List.of(text), 0, 2).getFirst();

            assertEquals(1, box.width() % 2, "width is odd so the arrow can be centred");
            assertEquals(-(box.width() / 2), box.x());
            assertTrue(box.width() >= text.width() + 2 * BubbleLayout.CORNER - 2, "text fits horizontally");
            assertTrue(box.textX() >= box.x() && box.textX() + text.width() <= box.x() + box.width());
            int textHeight = text.lines() * BubbleLayout.LINE_HEIGHT - 1;
            assertTrue(box.textY() >= box.y() && box.textY() + textHeight <= box.bottom(), "text fits vertically");
        }
    }

    @Test
    void paddingMakesBubblesBigger() {
        Box tight = BubbleLayout.stack(List.of(new TextSize(40, 1)), 0, 2).getFirst();
        Box padded = BubbleLayout.stack(List.of(new TextSize(40, 1)), 3, 2).getFirst();

        assertTrue(padded.width() > tight.width());
        assertTrue(padded.height() > tight.height());
    }
}
