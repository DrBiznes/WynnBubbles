package net.wynnbubbles.accessor;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.wynnbubbles.util.RenderBubble.ChatType;

public interface AbstractClientPlayerEntityAccessor {
    public void setChatText(List<String> text, int currentAge, int width, int height, ChatType chatType);

    @Nullable
    public List<String> getChatText();

    public int getOldAge();

    public int getWidth();

    public int getHeight();

    public ChatType getChatType();
}