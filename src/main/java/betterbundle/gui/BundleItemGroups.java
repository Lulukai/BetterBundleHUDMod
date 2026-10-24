package betterbundle.gui;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Group by vanilla stack identity, including every data component, but excluding count. */
public final class BundleItemGroups {
    private BundleItemGroups() {}

    public static final class Key {
        private final ItemStack stack;
        private final int hash;
        public Key(ItemStack stack) {
            this.stack = stack.copyWithCount(1);
            this.hash = ItemStack.hashItemAndComponents(this.stack);
        }
        public ItemStack stack() { return stack.copy(); }
        public boolean matches(ItemStack other) { return ItemStack.isSameItemSameComponents(stack, other); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) { return other instanceof Key key && matches(key.stack); }
    }

    public record Source(int bundleSlot, int itemIndex, ItemStack stack) {}
    public record Group(Key key, ItemStack stack, int totalCount, List<Source> sources) {}

    public static List<Group> combine(List<Source> sources) {
        var grouped = new LinkedHashMap<Key, List<Source>>();
        for (Source source : sources) {
            if (!source.stack().isEmpty()) grouped.computeIfAbsent(new Key(source.stack()), ignored -> new ArrayList<>()).add(source);
        }
        List<Group> result = new ArrayList<>();
        grouped.forEach((key, entries) -> result.add(new Group(key, key.stack(),
                entries.stream().mapToInt(entry -> entry.stack().getCount()).sum(), List.copyOf(entries))));
        return List.copyOf(result);
    }
}
