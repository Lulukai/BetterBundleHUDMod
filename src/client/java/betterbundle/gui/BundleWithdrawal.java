package betterbundle.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import betterbundle.util.BundleContentsHelper;

/** Minecraft adapter for grouped withdrawals. No fabricated click packets or local inventory edits. */
public final class BundleWithdrawal {
    private static WithdrawalSession<BundleItemGroups.Key> session;
    private BundleWithdrawal() {}

    public static boolean busy() { return session != null && session.active(); }
    public static void tick() {
        if (session != null) {
            session.tick();
            if (!session.active()) session = null;
        }
    }

    public static boolean start(BundleItemGroups.Group group, int button, boolean shift, AbstractContainerScreen<?> screen) {
        if (busy()) return true;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null || client.getConnection() == null) return false;
        Access access = new Access(client, screen, shift && button == 0 && !(screen instanceof InventoryScreen));
        if (!access.valid() || !client.player.containerMenu.getCarried().isEmpty()) return false;
        // Right-click retains vanilla's single bundle-entry extraction, without a staging slot.
        if (!shift && button == 1) {
            var source = access.findSource(group.key());
            if (source != null) access.extract(source, group.key());
            return true;
        }
        if (button != 0 && button != 1) return false;
        session = new WithdrawalSession<>(access, group.key(), group.totalCount(), group.stack().getMaxStackSize(), !shift);
        if (!session.active()) {
            session = null;
            client.player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.better-bundle.no_staging_slot"));
        }
        return true;
    }

    private static final class Access implements WithdrawalSession.Access<BundleItemGroups.Key> {
        private final Minecraft client;
        private final AbstractContainerScreen<?> screen;
        private final AbstractContainerMenu menu;
        private final net.minecraft.client.player.LocalPlayer player;
        private final ClientPacketListener connection;
        private final boolean containerFirst;
        Access(Minecraft client, AbstractContainerScreen<?> screen, boolean containerFirst) {
            this.client = client;
            this.screen = screen;
            this.player = client.player;
            this.menu = player.containerMenu;
            this.connection = client.getConnection();
            this.containerFirst = containerFirst;
        }
        public boolean valid() {
            return client.player == player && player.containerMenu == menu && screen.getMenu() == menu && client.gui.screen() == screen
                    && client.getConnection() == connection && client.gameMode != null;
        }
        public WithdrawalSession.Source findSource(BundleItemGroups.Key key) {
            for (var bundle : BundlePanelRenderer.getBundles()) {
                if (!menu.isValidSlotIndex(bundle.bundleSlot()) || !menu.getSlot(bundle.bundleSlot()).mayPickup(player)) continue;
                var items = bundle.contents().itemCopies().toList();
                for (int i = 0; i < items.size(); i++) {
                    if (key.matches(items.get(i))) return new WithdrawalSession.Source(bundle.bundleSlot(), i, items.get(i).getCount());
                }
            }
            return null;
        }
        private boolean playerStorage(Slot slot) {
            return slot.container == player.getInventory() && slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 36;
        }
        public int findEmptySlot(BundleItemGroups.Key key) {
            for (Slot slot : menu.slots) {
                if (playerStorage(slot) && !slot.hasItem() && slot.mayPlace(key.stack()) && slot.mayPickup(player)
                        && slot.getMaxStackSize(key.stack()) >= key.stack().getMaxStackSize()) return slot.index;
            }
            return -1;
        }
        public int findDestination(BundleItemGroups.Key key) {
            // Prefer matching partial stacks, then empty slots. Never feed another bundle accidentally.
            for (int region = 0; region < (containerFirst ? 2 : 1); region++) {
                boolean container = containerFirst && region == 0;
                for (int empty = 0; empty < 2; empty++) {
                    for (Slot slot : menu.slots) {
                        if (container ? slot.container == player.getInventory() : !playerStorage(slot)) continue;
                        ItemStack item = slot.getItem();
                        if ((empty == 1) != item.isEmpty() || !slot.mayPlace(key.stack())) continue;
                        if (!item.isEmpty() && (item.getItem() instanceof BundleItem || !key.matches(item))) continue;
                        if (item.getCount() < Math.min(slot.getMaxStackSize(key.stack()), key.stack().getMaxStackSize())) return slot.index;
                    }
                }
            }
            return -1;
        }
        public int countInSlot(int slot, BundleItemGroups.Key key) {
            if (!menu.isValidSlotIndex(slot)) return -1;
            ItemStack item = menu.getSlot(slot).getItem();
            return item.isEmpty() ? 0 : key.matches(item) ? item.getCount() : -1;
        }
        public int carriedCount(BundleItemGroups.Key key) {
            ItemStack item = menu.getCarried();
            return item.isEmpty() ? 0 : key.matches(item) ? item.getCount() : -1;
        }
        public void extract(WithdrawalSession.Source source, BundleItemGroups.Key key) {
            if (!menu.getCarried().isEmpty() || !menu.isValidSlotIndex(source.bundleSlot())) return;
            ItemStack bundle = menu.getSlot(source.bundleSlot()).getItem();
            var contents = BundleContentsHelper.getContents(bundle);
            if (contents == null) return;
            var items = contents.itemCopies().toList();
            if (source.itemIndex() < 0 || source.itemIndex() >= items.size() || !key.matches(items.get(source.itemIndex()))) return;
            if (BundleItem.getSelectedItemIndex(bundle) != source.itemIndex()) {
                menu.setSelectedBundleItemIndex(source.bundleSlot(), source.itemIndex());
                connection.send(new ServerboundSelectBundleItemPacket(source.bundleSlot(), source.itemIndex()));
            }
            click(source.bundleSlot(), 1);
        }
        public void deposit(int slot) {
            if (!valid() || !menu.isValidSlotIndex(slot)) return;
            ItemStack carried = menu.getCarried();
            Slot destination = menu.getSlot(slot);
            ItemStack existing = destination.getItem();
            if (carried.isEmpty() || !destination.mayPlace(carried)) return;
            if (!existing.isEmpty() && (existing.getItem() instanceof BundleItem
                    || !ItemStack.isSameItemSameComponents(existing, carried))) return;
            click(slot, 0);
        }
        public void returnRemainder(int slot) {
            // A held bundle's primary click could instead pull the source bundle into it.
            // Keep such an unexpected item visible on the cursor rather than nesting/swapping it.
            if (menu.getCarried().getItem() instanceof BundleItem) return;
            if (menu.isValidSlotIndex(slot) && BundleContentsHelper.isBundle(menu.getSlot(slot).getItem())) click(slot, 0);
        }
        public void pickup(int slot) { click(slot, 0); }
        private void click(int slot, int button) {
            if (valid() && menu.isValidSlotIndex(slot)) client.gameMode.handleContainerInput(menu.containerId, slot, button, ContainerInput.PICKUP, player);
        }
    }
}
