package net.wynnbubbles.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.wynnbubbles.WynnBubbles;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes {@link WynnBubblesConfig} as a commented JSON5 file without any config library.
 * The file name and format match what Cloth Config's AutoConfig used to write, so existing files keep working.
 */
public final class ConfigManager {

    private static final Gson GSON = new Gson();

    private ConfigManager() {
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("wynnbubbles.json5");
    }

    /** The public, non-static option fields in declaration order. */
    public static List<Field> options() {
        List<Field> fields = new ArrayList<>();
        for (Field field : WynnBubblesConfig.class.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (Modifier.isPublic(mods) && !Modifier.isStatic(mods) && !Modifier.isTransient(mods)) {
                fields.add(field);
            }
        }
        return fields;
    }

    /**
     * Loads the config file, or the defaults if there is none. A file that fails to parse is left
     * untouched (so hand edits aren't lost) and the defaults are used for this session.
     */
    public static WynnBubblesConfig load() {
        return load(path());
    }

    public static WynnBubblesConfig load(Path path) {
        WynnBubblesConfig config = new WynnBubblesConfig();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                // JsonParser is lenient: it accepts the /* */ and // comments in the file
                JsonElement json = JsonParser.parseReader(reader);
                WynnBubblesConfig parsed = GSON.fromJson(json, WynnBubblesConfig.class);
                if (parsed != null) {
                    config = parsed;
                }
            } catch (Exception e) {
                WynnBubbles.LOGGER.error("Could not read {}, using default settings until it is fixed", path, e);
                return config;
            }
        }
        // Rewrite so options added in newer versions show up in the file
        save(config, path);
        return config;
    }

    public static void save(WynnBubblesConfig config) {
        save(config, path());
    }

    public static void save(WynnBubblesConfig config, Path path) {
        StringBuilder out = new StringBuilder("{\n");
        List<Field> fields = options();
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null) {
                out.append("\t/* ").append(comment.value()).append(" */\n");
            }
            Object value;
            try {
                value = field.get(config);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
            out.append('\t').append(GSON.toJson(field.getName())).append(": ").append(GSON.toJson(value));
            out.append(i < fields.size() - 1 ? ",\n" : "\n");
        }
        out.append("}\n");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            WynnBubbles.LOGGER.error("Could not save {}", path, e);
        }
    }
}
