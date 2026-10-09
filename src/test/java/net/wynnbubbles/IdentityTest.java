package net.wynnbubbles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.wynnbubbles.identity.PlayerIds;
import net.wynnbubbles.identity.SenderMatcher;
import org.junit.jupiter.api.Test;

class IdentityTest {
    private static final UUID ALICE = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID ALICE_GHOST = UUID.fromString("11111111-1111-2111-8111-111111111111");
    private static final UUID BOB = UUID.fromString("22222222-2222-4222-8222-222222222222");

    private record Candidate(String name, UUID entityId) {
        UUID canonicalId() {
            return PlayerIds.canonical(entityId);
        }
    }

    @Test
    void realPlayerUuidIsLeftAlone() {
        assertEquals(ALICE, PlayerIds.canonical(ALICE));
    }

    @Test
    void ghostUuidFoldsBackToAccountUuid() {
        assertEquals(2, ALICE_GHOST.version());
        assertEquals(ALICE, PlayerIds.canonical(ALICE_GHOST));
    }

    @Test
    void offlineModeUuidIsLeftAlone() {
        UUID offline = UUID.nameUUIDFromBytes("OfflinePlayer:Player0".getBytes());

        assertEquals(3, offline.version());
        assertEquals(offline, PlayerIds.canonical(offline));
    }

    @Test
    void nullUuidStaysNull() {
        assertNull(PlayerIds.canonical(null));
    }

    @Test
    void ghostEntityIsMatchedByTabListUuid() {
        // the entity carries the ghost UUID and a name that does not match the chat line
        Candidate ghost = new Candidate("", ALICE_GHOST);
        Candidate bob = new Candidate("Bob", BOB);

        Optional<Candidate> match =
                SenderMatcher.find("Alice", ALICE, List.of(bob, ghost), Candidate::name, Candidate::canonicalId);

        assertEquals(Optional.of(ghost), match);
    }

    @Test
    void fallsBackToNameWhenSenderIsNotInTabList() {
        Candidate alice = new Candidate("Alice", ALICE);
        Candidate bob = new Candidate("Bob", BOB);

        Optional<Candidate> match =
                SenderMatcher.find("alice", null, List.of(bob, alice), Candidate::name, Candidate::canonicalId);

        assertEquals(Optional.of(alice), match);
    }

    @Test
    void uuidMatchWinsOverSomeoneElseWithTheSameName() {
        Candidate impostor = new Candidate("Alice", BOB);
        Candidate real = new Candidate("Alice", ALICE_GHOST);

        Optional<Candidate> match =
                SenderMatcher.find("Alice", ALICE, List.of(impostor, real), Candidate::name, Candidate::canonicalId);

        assertEquals(Optional.of(real), match);
    }

    @Test
    void nobodyNearbyMeansNoMatch() {
        Candidate bob = new Candidate("Bob", BOB);

        assertTrue(SenderMatcher.find("Alice", ALICE, List.of(bob), Candidate::name, Candidate::canonicalId)
                .isEmpty());
    }
}
