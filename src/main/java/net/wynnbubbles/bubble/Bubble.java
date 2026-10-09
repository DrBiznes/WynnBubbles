package net.wynnbubbles.bubble;

import net.minecraft.network.chat.Component;
import net.wynnbubbles.util.ChatType;

public record Bubble(Component text, ChatType type, long createdTick) {}
