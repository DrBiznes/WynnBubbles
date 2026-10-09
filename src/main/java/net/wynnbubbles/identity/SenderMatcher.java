package net.wynnbubbles.identity;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public final class SenderMatcher {

    /**
     * Picks the player a chat line belongs to.
     *
     * @param tabListId the sender's tab-list UUID, or null when the name is not in the tab list
     * @param idOf      canonical account id of a candidate (see {@link PlayerIds#canonical})
     */
    public static <P> Optional<P> find(
            String senderName, UUID tabListId, Iterable<P> candidates, Function<P, String> nameOf, Function<P, UUID> idOf) {
        UUID wanted = PlayerIds.canonical(tabListId);
        P byName = null;
        for (P candidate : candidates) {
            if (wanted != null && wanted.equals(idOf.apply(candidate))) return Optional.of(candidate);
            if (byName == null && senderName.equalsIgnoreCase(nameOf.apply(candidate))) byName = candidate;
        }
        return Optional.ofNullable(byName);
    }

    private SenderMatcher() {}
}
