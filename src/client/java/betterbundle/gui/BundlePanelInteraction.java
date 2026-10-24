package betterbundle.gui;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;

import betterbundle.util.BundleContentsHelper;

public final class BundlePanelInteraction {


    private BundlePanelInteraction() {}

    private static BundleItemGroups.Group getClickedItem(double mouseX, double mouseY,
                                                                int leftPos, int topPos, int imageHeight) {
        List<BundlePanelRenderer.BundleSlotEntry> bundles = BundlePanelRenderer.getBundles();
        if (bundles.isEmpty()) return null;

        List<BundleItemGroups.Group> allItems = BundlePanelRenderer.buildGroupedItemList(bundles);
        if (allItems.isEmpty()) return null;

        // Use filtered items to match rendered panel
        List<BundleItemGroups.Group> items = BundlePanelRenderer.filterItems(allItems);
        if (items.isEmpty()) return null;

        BundlePanelLayout layout = BundlePanelRenderer.layout(leftPos, topPos, imageHeight);
        int cell = layout.cellAt(mouseX, mouseY);
        if (cell < 0) return null;
        int flatIndex = BundlePanelRenderer.getScrollOffset() * BundlePanelRenderer.COLUMNS + cell;
        if (flatIndex >= items.size()) return null;
        return items.get(flatIndex);
    }

    public static boolean handlePanelClick(double mouseX, double mouseY, int button, int modifiers,
                                            int leftPos, int topPos,
                                            net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
        BundleItemGroups.Group clicked = getClickedItem(mouseX, mouseY, leftPos, topPos, screen.imageHeight);
        if (clicked == null) return false;
        return BundleWithdrawal.start(clicked, button, (modifiers & InputConstants.MOD_SHIFT) != 0, screen);
    }

    public static boolean handleSpaceClick(Slot hoveredSlot) {
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return false;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getWindow() == null) return false;

        if (!InputConstants.isKeyDown(InputConstants.KEY_SPACE)) return false;

        Player player = client.player;
        ItemStack stack = hoveredSlot.getItem();
        if (stack.isEmpty() || BundleContentsHelper.isNonEmptyBundle(stack)) return false;

        List<BundlePanelRenderer.BundleSlotEntry> bundles = BundlePanelRenderer.getAllBundles();
        int targetBundleSlot = -1;
        for (BundlePanelRenderer.BundleSlotEntry entry : bundles) {
            if (BundleContentsHelper.canFitItem(entry.bundleStack(), stack)) {
                targetBundleSlot = entry.bundleSlot();
                break;
            }
        }
        if (targetBundleSlot < 0) return false;

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return false;

        int containerId = player.containerMenu.containerId;
        int itemSlot = hoveredSlot.index;

        connection.send(makeClickPacket(containerId, itemSlot, (byte) 0));
        connection.send(makeClickPacket(containerId, targetBundleSlot, (byte) 0));

        return true;
    }

    private static ServerboundContainerClickPacket makeClickPacket(int containerId, int slot, byte button) {
        return new ServerboundContainerClickPacket(
                containerId, -1, (short) slot, button,
                ContainerInput.PICKUP, new Int2ObjectOpenHashMap<>(), HashedStack.EMPTY);
    }

    private static long bulkInsertStart = 0;
    private static final long BULK_INSERT_DELAY = 50; // 0.05s

    /** Start the bulk-insert timer (called on space+left-click inside panel with empty cursor). */
    public static void startBulkInsert() {
        bulkInsertStart = System.currentTimeMillis();
    }

    /** Whether the bulk-insert state is active (left button held > 0.05s). */
    public static boolean isBulkInsertActive() {
        return bulkInsertStart > 0 && (System.currentTimeMillis() - bulkInsertStart) >= BULK_INSERT_DELAY;
    }

    /** Exit bulk-insert state. */
    public static void stopBulkInsert() {
        bulkInsertStart = 0;
    }

    /** Put cursor item into any available bundle.
     *  button 0 = left (insert all), 1 = right (insert one). */
    public static boolean handlePanelInsert(int button) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return false;

        ItemStack cursor = player.containerMenu.getCarried();
        if (cursor.isEmpty()) return false;

        List<BundlePanelRenderer.BundleSlotEntry> bundles = BundlePanelRenderer.getAllBundles();
        int targetBundleSlot = -1;
        for (BundlePanelRenderer.BundleSlotEntry entry : bundles) {
            if (BundleContentsHelper.canFitItem(entry.bundleStack(), cursor)) {
                targetBundleSlot = entry.bundleSlot();
                break;
            }
        }
        if (targetBundleSlot < 0) return false;

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return false;
        int containerId = player.containerMenu.containerId;
        connection.send(makeClickPacket(containerId, targetBundleSlot, (byte) button));
        return true;
    }

    public static boolean handleScroll(double mouseX, double mouseY, double scrollDelta,
                                        int leftPos, int topPos, int imageHeight) {
        if (!BundlePanelRenderer.isEffectivelyVisible()) return false;
        if (!isInsidePanel(mouseX, mouseY, leftPos, topPos, imageHeight)) return false;
        BundlePanelRenderer.scrollBy(scrollDelta > 0 ? -1 : 1, imageHeight);
        return true;
    }

    public static boolean isInsidePanel(double mouseX, double mouseY,
                                         int leftPos, int topPos, int imageHeight) {
        return BundlePanelRenderer.layout(leftPos, topPos, imageHeight).containsGrid(mouseX, mouseY);
    }
}