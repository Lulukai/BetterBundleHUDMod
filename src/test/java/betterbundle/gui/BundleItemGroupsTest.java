package betterbundle.gui;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BundleItemGroupsTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // 26.3 binds defaults when a world/server is loaded. Supply the common
        // defaults for these headless fixtures, then exercise the real stack API.
        Items.STONE.builtInRegistryHolder().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
        Items.DIRT.builtInRegistryHolder().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
        Items.DIAMOND_PICKAXE.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                .addAll(DataComponents.COMMON_ITEM_COMPONENTS)
                .set(DataComponents.MAX_STACK_SIZE, 1).set(DataComponents.MAX_DAMAGE, 1561)
                .set(DataComponents.DAMAGE, 0).build());
    }

    private BundleItemGroups.Source source(int bundle, int index, ItemStack stack) {
        return new BundleItemGroups.Source(bundle, index, stack);
    }

    @Test
    void identicalItemsFromDifferentBundlesShareOneTotalBeyondAStack() {
        var groups = BundleItemGroups.combine(List.of(source(9, 0, new ItemStack(Items.STONE, 40)),
                source(10, 2, new ItemStack(Items.STONE, 35))));
        assertEquals(1, groups.size());
        assertEquals(75, groups.getFirst().totalCount());
        assertEquals(2, groups.getFirst().sources().size());
        assertEquals(1, groups.getFirst().stack().getCount());
    }

    @Test
    void differentNamesAndDurabilityRemainSeparate() {
        ItemStack named = new ItemStack(Items.STONE, 3);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("My stone"));
        ItemStack toolA = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack toolB = toolA.copy();
        toolB.set(DataComponents.DAMAGE, 12);
        var groups = BundleItemGroups.combine(List.of(source(9, 0, new ItemStack(Items.STONE, 2)),
                source(9, 1, named), source(10, 0, toolA), source(11, 0, toolB)));
        assertEquals(4, groups.size());
    }

    @Test
    void componentDifferencesArePartOfIdentityAndKeysIgnoreCount() {
        ItemStack a = new ItemStack(Items.STONE, 2);
        ItemStack b = a.copyWithCount(32);
        assertEquals(new BundleItemGroups.Key(a), new BundleItemGroups.Key(b));
        assertEquals(new BundleItemGroups.Key(a).hashCode(), new BundleItemGroups.Key(b).hashCode());
        b.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        assertNotEquals(new BundleItemGroups.Key(a), new BundleItemGroups.Key(b));
    }

    @Test
    void groupOrderIsStableAndEmptyEntriesAreIgnored() {
        var groups = BundleItemGroups.combine(List.of(source(9, 0, new ItemStack(Items.STONE)),
                source(9, 1, ItemStack.EMPTY), source(10, 0, new ItemStack(Items.DIRT)),
                source(11, 0, new ItemStack(Items.STONE))));
        assertEquals(2, groups.size());
        assertTrue(groups.getFirst().stack().is(Items.STONE));
        assertEquals(2, groups.getFirst().totalCount());
        assertTrue(groups.get(1).stack().is(Items.DIRT));
    }
}
