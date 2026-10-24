package betterbundle.gui;

/** Pixel positions shared by drawing and mouse input. No client state is needed. */
public record BundlePanelLayout(int x, int y, int height) {
    public static final int COLUMNS = 6;
    public static final int SLOT_SIZE = 18;
    public static final int MAX_ROWS = 8;
    public static final int TAB_WIDTH = 20;
    public static final int TAB_HEIGHT = 20;
    public static final int BODY_WIDTH = 130;
    public static final int WIDTH = TAB_WIDTH + BODY_WIDTH;
    public static final int SCROLL_WIDTH = 12;
    public static final int HEADER_HEIGHT = 12;

    public static BundlePanelLayout at(int leftPos, int topPos, int imageHeight) {
        return new BundlePanelLayout(leftPos - WIDTH - 4, topPos, Math.max(166, imageHeight));
    }

    public int bodyX() { return x + TAB_WIDTH; }
    public int gridX() { return bodyX() + 4; }
    public int gridY() { return y + 22; }
    public int rows() { return Math.min(MAX_ROWS, (height - 40) / SLOT_SIZE); }
    public int gridWidth() { return COLUMNS * SLOT_SIZE; }
    public int gridHeight() { return rows() * SLOT_SIZE; }
    public int scrollX() { return gridX() + gridWidth() + 2; }
    public int headerX() { return gridX(); }
    public int headerY() { return y + 7; }
    public int headerWidth() { return gridWidth(); }
    public int tabY(int index) { return y + 3 + index * TAB_HEIGHT; }

    public boolean contains(double mx, double my) {
        return inside(mx, my, x, y, WIDTH, height);
    }

    public boolean containsGrid(double mx, double my) {
        return inside(mx, my, gridX(), gridY(), gridWidth(), gridHeight());
    }

    public boolean containsTab(int index, double mx, double my) {
        return index >= 0 && index < 8 && inside(mx, my, x, tabY(index), TAB_WIDTH, TAB_HEIGHT);
    }

    /** Visible cell, or -1 for borders, tabs, the heading, and the scrollbar. */
    public int cellAt(double mx, double my) {
        if (!containsGrid(mx, my)) return -1;
        return (int) ((my - gridY()) / SLOT_SIZE) * COLUMNS
                + (int) ((mx - gridX()) / SLOT_SIZE);
    }

    private static boolean inside(double mx, double my, int x, int y, int width, int height) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }
}
