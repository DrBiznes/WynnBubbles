package net.wynnbubbles.test;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.wynnbubbles.WynnBubbles;
import net.wynnbubbles.bubble.Bubble;
import net.wynnbubbles.identity.PlayerIds;
import net.wynnbubbles.util.ChatType;

/**
 * Two fake remote players stand in front of the camera and "talk" through the real chat HUD, using
 * the line formats Wynncraft sends. Checks who gets which bubble, then takes screenshots for the
 * parts that can only be judged by eye (stacking, colours, position above nametags).
 *
 * <p>Wynntils is not loaded here: its events only fire on Wynncraft, so this covers the vanilla
 * fallback path plus the ghost UUID handling the two paths share.
 */
public class TwoPlayerBubbleTest implements FabricClientGameTest {
    private static final String GUILD = "󏿼󏿿󏿾";
    private static final String PARTY = "󏿼󏿿󏿾";
    private static final String PRIVATE = "󏿼󏿿󏿾";

    private static final UUID ALICE = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID BOB = UUID.fromString("22222222-2222-4222-8222-222222222222");
    /** Alice as Wynncraft would send her from another server: version nibble 2 instead of 4. */
    private static final UUID ALICE_GHOST = UUID.fromString("11111111-1111-2111-8111-111111111111");

    private static final int ALICE_ENTITY_ID = -9001;
    private static final int BOB_ENTITY_ID = -9002;
    private static final int GHOST_ENTITY_ID = -9003;

    /** Read by {@link net.wynnbubbles.test.mixin.ExtraNametagRowsMixin}. */
    public static volatile boolean extraNametagRows;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("tp @a ~ ~ ~ 0 0");
            context.waitTicks(10);

            context.runOnClient(mc -> {
                WynnBubbles.CONFIG.showOwnBubble = true;
                WynnBubbles.BUBBLES.clear();
                spawn(mc, ALICE_ENTITY_ID, ALICE, "Alice", -1.3);
                spawn(mc, BOB_ENTITY_ID, BOB, "Bob", 1.3);
            });
            context.waitTicks(5);

            // --- who gets which bubble ---
            context.runOnClient(mc -> {
                // Wynntils hands each line to one chat HUD per chat tab, so the same line arrives several times
                chat(mc, "Alice: hello there");
                chat(mc, "Alice: hello there");
                chat(mc, "Alice: hello there");
                chat(mc, GUILD + " Bob: guild meeting at 8");
                chat(mc, "Alice: this is a much longer message that has to wrap onto a second line of the bubble");
                chat(mc, PARTY + " Alice: party up");
                chat(mc, PRIVATE + " Bob  You: psst");
                chat(mc, "Carol: nobody by this name is nearby");
                chat(mc, "A system line that mentions Alice but has no colon");

                List<Bubble> alice = WynnBubbles.BUBBLES.get(ALICE);
                assertEquals(3, alice.size(), "Alice bubble count");
                assertEquals("party up", alice.get(0).text().getString(), "Alice newest bubble");
                assertEquals(ChatType.PARTY, alice.get(0).type(), "Alice newest bubble type");
                assertEquals(ChatType.NORMAL, alice.get(2).type(), "Alice oldest bubble type");
                assertEquals("hello there", alice.get(2).text().getString(), "Alice oldest bubble");

                List<Bubble> bob = WynnBubbles.BUBBLES.get(BOB);
                assertEquals(2, bob.size(), "Bob bubble count");
                assertEquals(ChatType.PRIVATE, bob.get(0).type(), "Bob newest bubble type");
                assertEquals(ChatType.GUILD, bob.get(1).type(), "Bob oldest bubble type");
                assertEquals("guild meeting at 8", bob.get(1).text().getString(), "Bob guild bubble");
            });
            context.waitTicks(5);
            context.takeScreenshot("1-two-players-stacked");

            // --- another mod adds rows above the name: bubbles must move above them ---
            extraNametagRows = true;
            context.waitTicks(5);
            context.takeScreenshot("2-above-extra-nametag-rows");
            extraNametagRows = false;

            // --- Alice hops servers and comes back as a ghost: her bubbles follow her ---
            context.runOnClient(mc -> {
                mc.level.removeEntity(ALICE_ENTITY_ID, Entity.RemovalReason.DISCARDED);
                spawn(mc, GHOST_ENTITY_ID, ALICE_GHOST, "Alice", -1.3);
                assertEquals(ALICE, PlayerIds.canonical(ALICE_GHOST), "ghost UUID folds back to the account UUID");
                chat(mc, "Alice: now talking as a ghost");

                List<Bubble> alice = WynnBubbles.BUBBLES.get(ALICE);
                assertEquals(4, alice.size(), "ghost Alice keeps her earlier bubbles");
                assertEquals("now talking as a ghost", alice.get(0).text().getString(), "ghost bubble");
            });
            context.waitTicks(5);
            context.takeScreenshot("3-ghost-keeps-bubbles");

            // --- own bubble, seen in third person ---
            context.runOnClient(mc -> {
                String self = mc.player.getGameProfile().name();
                chat(mc, self + ": talking to myself");
                assertEquals(1, WynnBubbles.BUBBLES.get(mc.player.getUUID()).size(), "own bubble count");
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            });
            context.waitTicks(5);
            context.takeScreenshot("4-own-bubble-third-person");
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

            // --- bubbles expire ---
            context.runOnClient(mc -> WynnBubbles.CONFIG.chatTime = 20);
            context.waitTicks(30);
            context.runOnClient(mc -> {
                assertEquals(0, WynnBubbles.BUBBLES.get(ALICE).size(), "Alice bubbles after expiry");
                assertEquals(0, WynnBubbles.BUBBLES.get(BOB).size(), "Bob bubbles after expiry");
            });
        }
    }

    private static void spawn(Minecraft mc, int entityId, UUID uuid, String name, double sideways) {
        RemotePlayer player = new RemotePlayer(mc.level, new GameProfile(uuid, name));
        player.setId(entityId);
        // the local player was turned to face +Z, so "in front" is +Z and the fake players face back at it
        player.setPos(mc.player.getX() + sideways, mc.player.getY(), mc.player.getZ() + 4.0);
        player.setYRot(180f);
        player.setYHeadRot(180f);
        player.setYBodyRot(180f);
        mc.level.addEntity(player);
    }

    private static void chat(Minecraft mc, String line) {
        mc.gui.getChat().addMessage(Component.literal(line));
    }

    private static void assertEquals(Object expected, Object actual, String what) {
        if (!expected.equals(actual)) {
            throw new AssertionError(what + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
