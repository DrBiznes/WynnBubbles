package net.wynnbubbles.util;

import net.minecraft.network.chat.Component;

/**
 * A single chat bubble message stored on a player entity.
 *
 * @param message       the cleaned message text as a Component
 * @param chatType      Wynncraft chat channel (normal/party/guild/private)
 * @param createdAtTick the player's tickCount when this message was received, used for expiry
 */
public record BubbleMessage(Component message, ChatType chatType, int createdAtTick) {}
