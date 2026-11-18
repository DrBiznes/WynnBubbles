package net.wynnbubbles.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.wynnbubbles.WynnBubbles;

import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * Builds the config screen with Cloth Config. Only loaded when Cloth Config is installed, so nothing
 * else in the mod may reference this class directly.
 */
public final class ClothConfigScreen {

    private static final String KEY_PREFIX = "text.autoconfig.wynnbubbles.option.";

    private ClothConfigScreen() {
    }

    public static Screen create(Screen parent) {
        WynnBubblesConfig config = WynnBubbles.CONFIG;
        WynnBubblesConfig defaults = new WynnBubblesConfig();
        Component title = Component.translatable("text.autoconfig.wynnbubbles.title");

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(title)
                .setDefaultBackgroundTexture(Identifier.parse("minecraft:textures/block/stone.png"))
                .setSavingRunnable(() -> ConfigManager.save(config));
        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(title);

        for (Field field : ConfigManager.options()) {
            FieldBuilder<?, ?, ?> entry = entry(entries, field, config, defaults);
            if (entry != null) {
                category.addEntry(entry.build());
            }
        }
        return builder.build();
    }

    private static FieldBuilder<?, ?, ?> entry(ConfigEntryBuilder entries, Field field, WynnBubblesConfig config, WynnBubblesConfig defaults) {
        Component label = Component.translatable(KEY_PREFIX + field.getName());
        Comment comment = field.getAnnotation(Comment.class);
        Component[] tooltip = comment == null ? new Component[0] : new Component[]{Component.literal(comment.value())};
        Class<?> type = field.getType();

        if (field.getName().equals("chatColor")) {
            return entries.startColorField(label, (int) get(field, config))
                    .setDefaultValue((int) get(field, defaults))
                    .setTooltip(tooltip)
                    .setSaveConsumer(setter(field, config));
        } else if (type == boolean.class) {
            return entries.startBooleanToggle(label, (boolean) get(field, config))
                    .setDefaultValue((boolean) get(field, defaults))
                    .setTooltip(tooltip)
                    .setSaveConsumer(setter(field, config));
        } else if (type == int.class) {
            return entries.startIntField(label, (int) get(field, config))
                    .setDefaultValue((int) get(field, defaults))
                    .setTooltip(tooltip)
                    .setSaveConsumer(setter(field, config));
        } else if (type == float.class) {
            return entries.startFloatField(label, (float) get(field, config))
                    .setDefaultValue((float) get(field, defaults))
                    .setTooltip(tooltip)
                    .setSaveConsumer(setter(field, config));
        } else if (type == double.class) {
            return entries.startDoubleField(label, (double) get(field, config))
                    .setDefaultValue((double) get(field, defaults))
                    .setTooltip(tooltip)
                    .setSaveConsumer(setter(field, config));
        }
        WynnBubbles.LOGGER.warn("No config screen entry for option {} of type {}", field.getName(), type);
        return null;
    }

    private static Object get(Field field, WynnBubblesConfig config) {
        try {
            return field.get(config);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> Consumer<T> setter(Field field, WynnBubblesConfig config) {
        return value -> {
            try {
                field.set(config, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        };
    }
}
