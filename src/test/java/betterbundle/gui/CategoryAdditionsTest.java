package betterbundle.gui;

import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class CategoryAdditionsTest {
    @Test
    void everyItemAddedSince261HasExactlyOneAssignment() throws Exception {
        // Independent fixture: item-asset difference between Mojang's 26.1 and 26.3 client JARs.
        Set<String> expected;
        try (var reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/betterbundle/new-items-since-26.1.ids"), StandardCharsets.UTF_8))) {
            expected = reader.lines().collect(Collectors.toSet());
        }
        assertEquals(152, expected.size());
        Set<String> actual = new HashSet<>();
        var allowedCategories = Set.of("BLOCKS", "DECORATIONS", "PLANTS", "FOOD", "TOOLS_AND_EQUIPMENT", "MISC", "MINERALS");
        CategoryAdditions.byCategory().forEach((category, ids) -> {
            assertTrue(allowedCategories.contains(category), "Unknown category: " + category);
            for (String id : ids) assertTrue(actual.add(id), "Duplicate assignment: " + id);
        });
        assertEquals(expected, actual, "All additions must appear in a category, not just All Items");
    }

    @Test
    void everyColorOfTheNewBuildingShapesAndCushionsAppearsInDecorations() {
        var decorations = CategoryAdditions.byCategory().get("DECORATIONS");
        for (String color : new String[] {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"}) {
            for (String suffix : new String[] {"wool_slab", "wool_stairs", "concrete_slab", "concrete_stairs", "cushion"}) {
                assertTrue(decorations.contains("minecraft:" + color + "_" + suffix));
            }
        }
    }

    @Test
    void newItemsFollowTheSameGroupingAsTheirExistingCounterparts() {
        var categories = CategoryAdditions.byCategory();
        assertTrue(categories.get("BLOCKS").containsAll(Set.of("minecraft:poplar_planks", "minecraft:poplar_log",
                "minecraft:sulfur", "minecraft:cinnabar", "minecraft:potent_sulfur")));
        assertTrue(categories.get("DECORATIONS").containsAll(Set.of("minecraft:poplar_door", "minecraft:poplar_shelf",
                "minecraft:cinnabar_brick_wall", "minecraft:straw_bed", "minecraft:sulfur_spike")));
        assertTrue(categories.get("PLANTS").containsAll(Set.of("minecraft:poplar_sapling", "minecraft:red_shrub",
                "minecraft:shelf_mushroom", "minecraft:orange_poplar_leaves")));
        assertTrue(categories.get("TOOLS_AND_EQUIPMENT").containsAll(Set.of("minecraft:poplar_boat",
                "minecraft:poplar_chest_boat", "minecraft:sulfur_cube_bucket", "minecraft:abandoned_camp_map")));
        assertTrue(categories.get("MISC").containsAll(Set.of("minecraft:music_disc_bounce", "minecraft:sulfur_cube_spawn_egg")));
    }
}
