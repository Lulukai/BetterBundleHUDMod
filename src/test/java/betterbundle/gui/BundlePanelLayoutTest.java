package betterbundle.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BundlePanelLayoutTest {
    @Test
    void survivalPanelMatchesInventoryHeightAndKeepsEveryControlInside() {
        var layout = BundlePanelLayout.at(200, 40, 166);
        assertEquals(166, layout.height());
        assertEquals(7, layout.rows());
        assertEquals(196, layout.x() + BundlePanelLayout.WIDTH);
        assertTrue(layout.gridY() >= layout.headerY() + BundlePanelLayout.HEADER_HEIGHT);
        assertTrue(layout.gridY() + layout.gridHeight() < layout.y() + layout.height() - 13);
        assertTrue(layout.scrollX() + BundlePanelLayout.SCROLL_WIDTH < layout.x() + BundlePanelLayout.WIDTH);
        for (int i = 0; i < 8; i++) {
            assertTrue(layout.tabY(i) >= layout.y());
            assertTrue(layout.tabY(i) + BundlePanelLayout.TAB_HEIGHT <= layout.y() + layout.height());
            assertTrue(layout.containsTab(i, layout.x() + 10, layout.tabY(i) + 10));
        }
    }

    @Test
    void everyRenderedCellMapsToExactlyOneItemIncludingAtEdges() {
        for (int height : new int[] {133, 166, 222}) {
            var layout = BundlePanelLayout.at(200, 40, height);
            for (int cell = 0; cell < layout.rows() * BundlePanelLayout.COLUMNS; cell++) {
                int x = layout.gridX() + cell % BundlePanelLayout.COLUMNS * BundlePanelLayout.SLOT_SIZE;
                int y = layout.gridY() + cell / BundlePanelLayout.COLUMNS * BundlePanelLayout.SLOT_SIZE;
                assertEquals(cell, layout.cellAt(x, y));
                assertEquals(cell, layout.cellAt(x + 17.99, y + 17.99));
            }
        }
    }

    @Test
    void bordersHeadingTabsAndScrollbarCannotSelectAnItem() {
        var layout = BundlePanelLayout.at(200, 40, 166);
        assertEquals(-1, layout.cellAt(layout.gridX() - 0.1, layout.gridY()));
        assertEquals(-1, layout.cellAt(layout.gridX(), layout.gridY() - 0.1));
        assertEquals(-1, layout.cellAt(layout.gridX() + layout.gridWidth(), layout.gridY()));
        assertEquals(-1, layout.cellAt(layout.gridX(), layout.gridY() + layout.gridHeight()));
        assertEquals(-1, layout.cellAt(layout.headerX() + 3, layout.headerY() + 3));
        assertEquals(-1, layout.cellAt(layout.scrollX() + 1, layout.gridY()));
        assertEquals(-1, layout.cellAt(layout.x() + 5, layout.tabY(0) + 5));
    }

    @Test
    void tallerContainersUseAnAdditionalRowWithoutChangingSlotSize() {
        var layout = BundlePanelLayout.at(200, 40, 222);
        assertEquals(222, layout.height());
        assertEquals(8, layout.rows());
        assertEquals(18, BundlePanelLayout.SLOT_SIZE);
        assertEquals(6 * 18, layout.gridWidth());
    }
    @Test
    void categoryHeadingIsSeparateFromItemsAndTheScrollbar() {
        var layout = BundlePanelLayout.at(200, 40, 166);
        assertEquals(layout.gridX(), layout.headerX());
        assertTrue(layout.headerX() + layout.headerWidth() < layout.scrollX());
        assertTrue(layout.headerY() + BundlePanelLayout.HEADER_HEIGHT < layout.gridY());
        assertEquals(-1, layout.cellAt(layout.headerX() + 3, layout.headerY() + 3));
    }
}