package net.wynnbubbles.mixin;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.wynnbubbles.accessor.AbstractClientPlayerEntityAccessor;
import net.wynnbubbles.util.RenderBubble.ChatType;

@Environment(EnvType.CLIENT)
@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin implements AbstractClientPlayerEntityAccessor {
    @Nullable
    private List<String> chatTextList = null;
    private int oldAge = 0;
    private int width;
    private int height;
    private ChatType chatType = ChatType.NORMAL;

    @Override
    public void setChatText(List<String> textList, int currentAge, int width, int height, ChatType chatType) {
        this.chatTextList = textList;
        this.oldAge = currentAge;
        this.width = width;
        this.height = height;
        this.chatType = chatType;
    }

    @Nullable
    @Override
    public List<String> getChatText() {
        return this.chatTextList;
    }

    @Override
    public int getOldAge() {
        return this.oldAge;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public ChatType getChatType() {
        return this.chatType;
    }
}