package betterbundle.gui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Explicit assignments for the additions in Minecraft 26.2 and 26.3. */
public final class CategoryAdditions {
    private CategoryAdditions() {}

    public static Map<String, Set<String>> byCategory() {
        Properties assignments = new Properties();
        try (InputStream stream = CategoryAdditions.class.getResourceAsStream(
                "/betterbundle/categories/post-26.1.properties")) {
            if (stream == null) throw new IllegalStateException("Missing item category assignments");
            assignments.load(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load item category assignments", exception);
        }
        Map<String, Set<String>> categories = new HashMap<>();
        for (String path : assignments.stringPropertyNames()) {
            categories.computeIfAbsent(assignments.getProperty(path), ignored -> new HashSet<>())
                    .add("minecraft:" + path);
        }
        categories.replaceAll((category, ids) -> Set.copyOf(ids));
        return Collections.unmodifiableMap(categories);
    }
}
