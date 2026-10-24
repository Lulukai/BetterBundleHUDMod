package betterbundle.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.List;

import betterbundle.util.BundleContentsHelper;

public final class BundlePanelRenderer {

    public static final int SLOT_SIZE = BundlePanelLayout.SLOT_SIZE;
    public static final int COLUMNS = BundlePanelLayout.COLUMNS;
    private static final Identifier INVENTORY = Identifier.withDefaultNamespace("textures/gui/container/inventory.png");

    private static int scrollOffset = 0;
    public static boolean visible = true;

    private static int hoveredBundleSlot = -1;

    public static BundleCategory currentCategory = BundleCategory.ALL;

    private BundlePanelRenderer() {}

    /** bundleSlot = container slot index (for clickSlot packets) */
    public record BundleSlotEntry(int bundleSlot, ItemStack bundleStack, BundleContents contents) {}

    public static BundlePanelLayout layout(int leftPos, int topPos, int imageHeight) {
        return BundlePanelLayout.at(leftPos, topPos, imageHeight);
    }

    public static int getScrollOffset() { return scrollOffset; }
    public static void scrollToTop() { scrollOffset = 0; }

    public static void scrollBy(int delta, int imageHeight) {
        int count = filterItems(buildGroupedItemList(getBundles())).size();
        int totalRows = (count + COLUMNS - 1) / COLUMNS;
        int rows = layout(0, 0, imageHeight).rows();
        scrollOffset = Math.clamp(scrollOffset + delta, 0, Math.max(0, totalRows - rows));
    }

    public static List<BundleItemGroups.Group> buildGroupedItemList(List<BundleSlotEntry> bundles) {
        List<BundleItemGroups.Source> sources = new ArrayList<>();
        for (BundleSlotEntry bundle : bundles) {
            if (bundle.contents() == null) continue;
            List<ItemStack> items = bundle.contents().itemCopies().toList();
            for (int i = 0; i < items.size(); i++) sources.add(new BundleItemGroups.Source(bundle.bundleSlot(), i, items.get(i)));
        }
        return BundleItemGroups.combine(sources);
    }

    public static List<BundleItemGroups.Group> filterItems(List<BundleItemGroups.Group> items) {
        return items.stream().filter(item -> currentCategory.matches(
                BuiltInRegistries.ITEM.getKey(item.stack().getItem()).toString())).toList();
    }

    public static List<BundleSlotEntry> getBundles() { return findBundles(false); }
    public static List<BundleSlotEntry> getAllBundles() { return findBundles(true); }

    private static List<BundleSlotEntry> findBundles(boolean includeEmpty) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return List.of();
        List<BundleSlotEntry> result = new ArrayList<>();
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            boolean matches = includeEmpty
                    ? BundleContentsHelper.isBundle(stack)
                    : BundleContentsHelper.isNonEmptyBundle(stack);
            if (matches) {
                // Convert inventory index to container slot index
                int containerSlot = findContainerSlot(player, inv, i);
                if (containerSlot >= 0) result.add(new BundleSlotEntry(containerSlot, stack, BundleContentsHelper.getContents(stack)));
            }
        }
        return result;
    }

    /** Convert player inventory index (0-35) to container menu slot index. */
    private static int findContainerSlot(Player player, Inventory inv, int inventoryIndex) {
        for (net.minecraft.world.inventory.Slot slot : player.containerMenu.slots) {
            if (slot.container == inv && slot.getContainerSlot() == inventoryIndex) {
                return slot.index;
            }
        }
        return -1;
    }

    public static boolean isRecipeBookOpen() {
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() instanceof AbstractRecipeBookScreen<?> screen) return screen.recipeBookComponent.isVisible();
        return false;
    }

    public static int getHoveredBundleSlot() { return isEffectivelyVisible() ? hoveredBundleSlot : -1; }
    public static boolean isEffectivelyVisible() { return visible && !isRecipeBookOpen(); }

    public static void toggleVisible() {
        visible = !visible;
        hoveredBundleSlot = -1;
    }

    public static BundleCategory getCategoryAt(double mouseX, double mouseY, int leftPos, int topPos, int imageHeight) {
        BundlePanelLayout layout = layout(leftPos, topPos, imageHeight);
        BundleCategory[] categories = BundleCategory.values();
        for (int i = 0; i < categories.length; i++) {
            if (layout.containsTab(i, mouseX, mouseY)) return categories[i];
        }
        return null;
    }

    // Uses Minecraft's own assets so slots and controls follow resource packs.
    public static void render(GuiGraphicsExtractor graphics, int leftPos, int topPos, int imageHeight, int mouseX, int mouseY) {
        hoveredBundleSlot = -1;
        if (!isEffectivelyVisible()) return;
        List<BundleSlotEntry> bundles = getBundles();
        if (bundles.isEmpty()) { scrollOffset = 0; return; }
        List<BundleItemGroups.Group> items = filterItems(buildGroupedItemList(bundles));
        BundlePanelLayout layout = layout(leftPos, topPos, imageHeight);
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        int totalRows = (items.size() + COLUMNS - 1) / COLUMNS;
        int maxScroll = Math.max(0, totalRows - layout.rows());
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);

        drawFrame(graphics, layout.bodyX(), layout.y(), BundlePanelLayout.BODY_WIDTH, layout.height());
        BundleCategory[] categories = BundleCategory.values();
        for (int i = 0; i < categories.length; i++) {
            int tabY = layout.tabY(i);
            boolean selected = categories[i] == currentCategory;
            boolean hovered = layout.containsTab(i, mouseX, mouseY);
            if (selected) {
                drawFrame(graphics, layout.x(), tabY, BundlePanelLayout.TAB_WIDTH + 4, BundlePanelLayout.TAB_HEIGHT);
                graphics.fill(layout.bodyX(), tabY + 4, layout.bodyX() + 4, tabY + 16, 0xFFC6C6C6);
            } else {
                sprite(graphics, hovered ? "widget/button_highlighted" : "widget/button",
                        layout.x() + 2, tabY, BundlePanelLayout.TAB_WIDTH - 2, BundlePanelLayout.TAB_HEIGHT);
            }
            graphics.item(categories[i].getIcon(), layout.x() + 3, tabY + 2);
            if (hovered) graphics.setTooltipForNextFrame(font, categories[i].getDisplayName(), mouseX, mouseY);
        }

        graphics.enableScissor(layout.headerX(), layout.headerY(),
                layout.headerX() + layout.headerWidth(), layout.headerY() + BundlePanelLayout.HEADER_HEIGHT);
        graphics.text(font, currentCategory.getDisplayName(), layout.headerX(), layout.headerY() + 2, 0xFF404040, false);
        graphics.disableScissor();

        graphics.fill(layout.scrollX(), layout.gridY(), layout.scrollX() + BundlePanelLayout.SCROLL_WIDTH,
                layout.gridY() + layout.gridHeight(), 0xFF8B8B8B);
        int thumbY = layout.gridY() + (maxScroll == 0 ? 0 : (layout.gridHeight() - 15) * scrollOffset / maxScroll);
        sprite(graphics, maxScroll > 0 ? "container/creative_inventory/scroller" : "container/creative_inventory/scroller_disabled",
                layout.scrollX(), thumbY, BundlePanelLayout.SCROLL_WIDTH, 15);

        int hoveredCell = layout.cellAt(mouseX, mouseY);
        for (int cell = 0; cell < layout.rows() * COLUMNS; cell++) {
            int x = layout.gridX() + cell % COLUMNS * SLOT_SIZE;
            int y = layout.gridY() + cell / COLUMNS * SLOT_SIZE;
            sprite(graphics, "container/slot", x, y, SLOT_SIZE, SLOT_SIZE);
            int itemIndex = scrollOffset * COLUMNS + cell;
            if (itemIndex >= items.size()) continue;
            BundleItemGroups.Group item = items.get(itemIndex);
            graphics.item(item.stack(), x + 1, y + 1);
            graphics.itemDecorations(font, item.stack(), x + 1, y + 1, "");
            if (item.totalCount() > 1) {
                String count = Integer.toString(item.totalCount());
                int width = font.width(count);
                float scale = Math.min(1.0f, 16.0f / width);
                var pose = graphics.pose();
                pose.pushMatrix();
                pose.translate(x + 17 - width * scale, y + 17 - (font.lineHeight - 2) * scale);
                pose.scale(scale, scale);
                graphics.text(font, count, 0, 0, 0xFFFFFFFF, true);
                pose.popMatrix();
            }
            if (cell == hoveredCell) {
                graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0x80FFFFFF);
                List<Component> tooltip = new ArrayList<>(item.stack().getTooltipLines(Item.TooltipContext.of(client.level),
                        client.player, client.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL));
                tooltip.add(Component.translatable("gui.better-bundle.group_total", item.totalCount(),
                        item.sources().stream().map(BundleItemGroups.Source::bundleSlot).distinct().count()));
                graphics.setTooltipForNextFrame(font, tooltip, item.stack().getTooltipImage(), mouseX, mouseY);
                hoveredBundleSlot = item.sources().getFirst().bundleSlot();
            }
        }

        int[] stats = getBundleStats();
        String countText = stats[0] + "/" + stats[1];
        graphics.text(font, countText, layout.bodyX() + BundlePanelLayout.BODY_WIDTH - 7 - font.width(countText),
                layout.y() + layout.height() - 13, 0xFF404040, false);
    }

    public static void sprite(GuiGraphicsExtractor graphics, String path, int x, int y, int width, int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(path), x, y, width, height);
    }

    /** Stretch only the empty frame of the vanilla inventory; never its slots or player preview. */
    public static void drawFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        int[] sourceX = {0, 4, 172};
        int[] sourceY = {0, 4, 162};
        int[] sourceWidths = {4, 168, 4};
        int[] sourceHeights = {4, 158, 4};
        int[] targetX = {x, x + 4, x + width - 4};
        int[] targetY = {y, y + 4, y + height - 4};
        int[] widths = {4, width - 8, 4};
        int[] heights = {4, height - 8, 4};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                boolean center = row == 1 && col == 1;
                graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY, targetX[col], targetY[row],
                        center ? 6 : sourceX[col], center ? 6 : sourceY[row], widths[col], heights[row],
                        center ? 1 : sourceWidths[col], center ? 1 : sourceHeights[row], 256, 256);
            }
        }
    }

    private static int[] getBundleStats() {
        List<BundleSlotEntry> all = getAllBundles();
        int totalItems = 0;
        Fraction totalWeight = Fraction.ZERO;
        for (BundleSlotEntry entry : all) {
            BundleContents c = entry.contents();
            if (c != null && !c.isEmpty()) {
                totalItems += c.itemCopies().mapToInt(ItemStack::getCount).sum();
                totalWeight = totalWeight.add(c.weight().result().orElse(Fraction.ZERO));
            }
        }
        // remaining weight → how many more "standard" items (weight 1/64) would fit
        Fraction maxWeight = Fraction.getFraction(all.size(), 1);
        Fraction remaining = maxWeight.subtract(totalWeight);
        int effectiveMax = totalItems + remaining.multiplyBy(Fraction.getFraction(64, 1)).intValue();
        return new int[] { totalItems, effectiveMax };
    }
}
