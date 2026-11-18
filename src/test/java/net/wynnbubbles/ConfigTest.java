package net.wynnbubbles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import net.wynnbubbles.config.ConfigManager;
import net.wynnbubbles.config.WynnBubblesConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {

    @TempDir
    Path dir;

    @Test
    void missingFileGivesDefaultsAndWritesCommentedFile() throws Exception {
        Path file = dir.resolve("wynnbubbles.json5");
        WynnBubblesConfig config = ConfigManager.load(file);

        assertEquals(new WynnBubblesConfig().chatTime, config.chatTime);
        String written = Files.readString(file);
        assertTrue(written.contains("/* Time in ticks"));
        assertTrue(written.contains("\"chatTime\": 260"));
    }

    @Test
    void readsFileWrittenByAutoConfig() throws Exception {
        // Shape of the file Cloth Config's JanksonConfigSerializer used to write; a missing option keeps its default
        Path file = dir.resolve("wynnbubbles.json5");
        Files.writeString(file, """
                {
                	/* Maximum distance in blocks at which chat bubbles are visible from other players */
                	"chatRange": 12.5,
                	// hand-written line comment
                	"chatTime": 100,
                	"backgroundOpacity": 0.25,
                	"showOwnBubble": false
                }
                """);

        WynnBubblesConfig config = ConfigManager.load(file);

        assertEquals(12.5, config.chatRange);
        assertEquals(100, config.chatTime);
        assertEquals(0.25f, config.backgroundOpacity);
        assertFalse(config.showOwnBubble);
        assertEquals(new WynnBubblesConfig().maxBubbles, config.maxBubbles);
        assertTrue(Files.readString(file).contains("\"maxBubbles\""), "missing options are added to the file");
    }

    @Test
    void savedFileRoundTrips() {
        Path file = dir.resolve("wynnbubbles.json5");
        WynnBubblesConfig config = new WynnBubblesConfig();
        config.chatScale = 1.75f;
        config.chatColor = 0xFFFFFF;
        config.debugMode = true;
        ConfigManager.save(config, file);

        WynnBubblesConfig loaded = ConfigManager.load(file);
        assertEquals(1.75f, loaded.chatScale);
        assertEquals(0xFFFFFF, loaded.chatColor);
        assertTrue(loaded.debugMode);
    }

    @Test
    void brokenFileIsLeftAlone() throws Exception {
        Path file = dir.resolve("wynnbubbles.json5");
        String broken = "{ \"chatTime\": 100, oops";
        Files.writeString(file, broken);

        WynnBubblesConfig config = ConfigManager.load(file);

        assertEquals(new WynnBubblesConfig().chatTime, config.chatTime);
        assertEquals(broken, Files.readString(file));
    }
}
