package net.wynnbubbles.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "wynnbubbles")
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class WynnBubblesConfig implements ConfigData {

    @Comment("Maximum distance in blocks at which chat bubbles are visible from other players")
    public double chatRange = 30.0D;

    @Comment("Time in ticks (20 ticks = 1 second) that chat bubbles remain visible")
    public int chatTime = 260;

    @Comment("Maximum width in pixels before text wraps to a new line. Higher values make wider bubbles")
    public int maxChatWidth = 180;

    @Comment("The color of the text in the chat bubble (in decimal format, e.g. 1315860 for white)")
    public int chatColor = 1315860;

    @Comment("Vertical offset of bubbles above players' heads. Positive values move up, negative values move down")
    public float chatHeight = 0.0f;

    @Comment("Size multiplier for chat bubbles (1.0 = normal size, 2.0 = double size, 0.5 = half size)")
    public float chatScale = 1.0f;

    @Comment("Transparency of the bubble background (0.0 = invisible, 1.0 = fully opaque)")
    public float backgroundOpacity = 0.7F;

    @Comment("Red component of normal chat bubble background color (0.0 to 1.0)")
    public float backgroundRed = 1.0F;

    @Comment("Green component of normal chat bubble background color (0.0 to 1.0)")
    public float backgroundGreen = 1.0F;

    @Comment("Blue component of normal chat bubble background color (0.0 to 1.0)")
    public float backgroundBlue = 1.0F;

    @Comment("Maximum number of words to check for player names in chat messages. 0 = unlimited. Lower values may improve performance")
    public int maxUUIDWordCheck = 0;

    @Comment("If enabled, shows chat bubbles above your own head when you chat")
    public boolean showOwnBubble = true;

    @Comment("Extra Y offset (in blocks) to push bubbles above Wynntils nametags. 0.5 covers base nametag + Wynntils marker. Increase if you have WynnTitles or extra nametag rows. Has no effect when Wynntils is not installed")
    public float wynntilsOffset = 0.5f;

    @Comment("Base vertical offset (in blocks) from the top of the player bounding box to the bottom of the lowest bubble")
    public float balloonsHeightOffset = 0.9f;

    @Comment("Maximum number of stacked chat bubbles shown above a player at once")
    public int maxBubbles = 5;

    @Comment("Pixel gap between stacked bubbles")
    public int distanceBetweenBubbles = 3;

    @Comment("Internal padding (in pixels) inside each bubble background")
    public int balloonPadding = 1;

    @Comment("Enable verbose debug logging to the game console (useful for diagnosing missing bubbles)")
    public boolean debugMode = false;
}