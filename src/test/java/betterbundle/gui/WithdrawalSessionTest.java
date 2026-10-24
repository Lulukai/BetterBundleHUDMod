package betterbundle.gui;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class WithdrawalSessionTest {
    static class Stack {
        String key; int count;
        Stack(String key, int count) { this.key = key; this.count = count; }
    }
    static class Inventory implements WithdrawalSession.Access<String> {
        Map<Integer, List<Stack>> bundles = new LinkedHashMap<>();
        List<Stack> slots = new ArrayList<>();
        Stack cursor;
        boolean valid = true, failExtract, failDeposit;
        int max = 64, extractions;
        Inventory(int... counts) { for (int count : counts) slots.add(count == 0 ? null : new Stack("stone", count)); }
        Inventory bundle(int slot, String key, int... counts) {
            var list = bundles.computeIfAbsent(slot, ignored -> new ArrayList<>());
            for (int count : counts) list.add(new Stack(key, count));
            return this;
        }
        public boolean valid() { return valid; }
        public WithdrawalSession.Source findSource(String key) {
            for (var entry : bundles.entrySet()) for (int i = 0; i < entry.getValue().size(); i++) {
                Stack stack = entry.getValue().get(i);
                if (stack.key.equals(key)) return new WithdrawalSession.Source(entry.getKey(), i, stack.count);
            }
            return null;
        }
        public int findEmptySlot(String key) { for (int i = 0; i < slots.size(); i++) if (slots.get(i) == null) return i; return -1; }
        public int findDestination(String key) {
            for (int i = 0; i < slots.size(); i++) if (slots.get(i) != null && slots.get(i).key.equals(key) && slots.get(i).count < max) return i;
            return findEmptySlot(key);
        }
        public int countInSlot(int slot, String key) { Stack s = slots.get(slot); return s == null ? 0 : s.key.equals(key) ? s.count : -1; }
        public int carriedCount(String key) { return cursor == null ? 0 : cursor.key.equals(key) ? cursor.count : -1; }
        public void extract(WithdrawalSession.Source source, String key) {
            assertNull(cursor);
            if (failExtract) return;
            Stack stack = bundles.get(source.bundleSlot()).get(source.itemIndex());
            assertEquals(key, stack.key, "Never use a stale index after removing a preceding item");
            cursor = bundles.get(source.bundleSlot()).remove(source.itemIndex());
            extractions++;
        }
        public void deposit(int slot) {
            if (failDeposit) return;
            Stack s = slots.get(slot);
            assertTrue(s == null || s.key.equals(cursor.key));
            int n = Math.min(cursor.count, max - (s == null ? 0 : s.count));
            if (s == null) slots.set(slot, new Stack(cursor.key, n)); else s.count += n;
            cursor.count -= n;
            if (cursor.count == 0) cursor = null;
        }
        public void returnRemainder(int bundleSlot) {
            assertNotNull(cursor);
            bundles.get(bundleSlot).add(0, cursor);
            cursor = null;
        }
        public void pickup(int slot) { assertNull(cursor); cursor = slots.set(slot, null); }
        int bundleCount(String key) { return bundles.values().stream().flatMap(List::stream).filter(s -> s.key.equals(key)).mapToInt(s -> s.count).sum(); }
        int stored(String key) { return slots.stream().filter(s -> s != null && s.key.equals(key)).mapToInt(s -> s.count).sum(); }
    }
    private void finish(WithdrawalSession<String> session) {
        for (int i = 0; session.active() && i < 100; i++) session.tick();
        assertFalse(session.active(), "Withdrawal must terminate");
    }

    @Test void normalClickCollectsOneLegalStackAcrossBundlesAndReturnsExcess() {
        var inv = new Inventory(0).bundle(9, "stone", 20).bundle(10, "stone", 30).bundle(11, "stone", 40);
        var session = new WithdrawalSession<>(inv, "stone", 90, 64, true);
        finish(session);
        assertEquals(64, inv.carriedCount("stone"));
        assertEquals(26, inv.bundleCount("stone"));
        assertEquals(0, inv.stored("stone"));
    }
    @Test void shiftClickWithdrawsAllAndFillsExistingStacksFirst() {
        var inv = new Inventory(60, 0, 0, 0).bundle(9, "stone", 64).bundle(10, "stone", 64).bundle(11, "stone", 32);
        finish(new WithdrawalSession<>(inv, "stone", 160, 64, false));
        assertEquals(0, inv.bundleCount("stone"));
        assertEquals(220, inv.stored("stone"));
        assertEquals(64, inv.slots.getFirst().count);
        assertNull(inv.cursor);
    }
    @Test void fullInventoryStopsAndReturnsUnstoredItemsToTheOriginalBundle() {
        var inv = new Inventory(59).bundle(9, "stone", 64).bundle(10, "stone", 12);
        finish(new WithdrawalSession<>(inv, "stone", 76, 64, false));
        assertEquals(64, inv.stored("stone"));
        assertEquals(71, inv.bundleCount("stone"));
        assertNull(inv.cursor);
        assertEquals(1, inv.extractions);
    }
    @Test void indexesAreResolvedAgainAndUnrelatedItemsStayInTheBundle() {
        var inv = new Inventory(0).bundle(9, "stone", 10).bundle(9, "dirt", 7).bundle(9, "stone", 12);
        finish(new WithdrawalSession<>(inv, "stone", 22, 64, false));
        assertEquals(22, inv.stored("stone"));
        assertEquals(7, inv.bundleCount("dirt"));
        assertEquals(0, inv.bundleCount("stone"));
    }
    @Test void closingOrChangingTheMenuCancelsAndLeavesCollectedItemsInInventory() {
        var inv = new Inventory(0).bundle(9, "stone", 10).bundle(10, "stone", 12);
        var session = new WithdrawalSession<>(inv, "stone", 22, 64, true);
        session.tick();
        inv.valid = false;
        session.tick();
        assertFalse(session.active());
        assertEquals(10, inv.stored("stone"));
        assertEquals(12, inv.bundleCount("stone"));
        assertNull(inv.cursor);
    }
    @Test void serverCorrectionToTheStagingSlotCancelsWithoutPickingItUp() {
        var inv = new Inventory(0).bundle(9, "stone", 10).bundle(10, "stone", 12);
        var session = new WithdrawalSession<>(inv, "stone", 22, 64, true);
        session.tick();
        inv.slots.set(0, new Stack("dirt", 4));
        session.tick();
        assertFalse(session.active());
        assertNull(inv.cursor);
        assertEquals(4, inv.stored("dirt"));
        assertEquals(12, inv.bundleCount("stone"));
    }
    @Test void unstackableItemsNeverCreateAnOversizedCursorStack() {
        var inv = new Inventory(0).bundle(9, "tool", 1).bundle(10, "tool", 1);
        inv.max = 1;
        finish(new WithdrawalSession<>(inv, "tool", 2, 1, true));
        assertEquals(1, inv.carriedCount("tool"));
        assertEquals(1, inv.bundleCount("tool"));
    }
    @Test void noFreeStagingSlotDoesNotTouchAnyBundle() {
        var inv = new Inventory(30).bundle(9, "stone", 20);
        var session = new WithdrawalSession<>(inv, "stone", 20, 64, true);
        assertFalse(session.active());
        assertEquals(20, inv.bundleCount("stone"));
    }
    @Test void failedExtractionStopsWithoutClickingDestinations() {
        var inv = new Inventory(0).bundle(9, "stone", 20);
        inv.failExtract = true;
        finish(new WithdrawalSession<>(inv, "stone", 20, 64, false));
        assertEquals(20, inv.bundleCount("stone"));
        assertEquals(0, inv.stored("stone"));
    }
    @Test void rejectedDepositReturnsTheEntireExtractedEntry() {
        var inv = new Inventory(0).bundle(9, "stone", 20);
        inv.failDeposit = true;
        finish(new WithdrawalSession<>(inv, "stone", 20, 64, false));
        assertEquals(20, inv.bundleCount("stone"));
        assertEquals(0, inv.stored("stone"));
        assertNull(inv.cursor);
    }
}
