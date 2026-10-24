package betterbundle.gui;

/** One source entry per tick; all inventory operations are delegated to vanilla clicks. */
public final class WithdrawalSession<K> {
    public record Source(int bundleSlot, int itemIndex, int count) {}
    public interface Access<K> {
        boolean valid();
        Source findSource(K key);
        int findEmptySlot(K key);
        int findDestination(K key);
        int countInSlot(int slot, K key);
        int carriedCount(K key);
        void extract(Source source, K key);
        void deposit(int slot);
        void returnRemainder(int bundleSlot);
        void pickup(int slot);
    }

    private final Access<K> access;
    private final K key;
    private final boolean toCursor;
    private final int staging;
    private final int target;
    private int moved;
    private boolean active = true;

    public WithdrawalSession(Access<K> access, K key, int totalCount, int maxStackSize, boolean toCursor) {
        this.access = access;
        this.key = key;
        this.toCursor = toCursor;
        this.target = toCursor ? Math.min(totalCount, maxStackSize) : totalCount;
        this.staging = toCursor ? access.findEmptySlot(key) : -1;
        if (target <= 0 || !access.valid() || access.carriedCount(key) != 0 || (toCursor && staging < 0)) active = false;
    }

    public boolean active() { return active; }
    public void cancel() { active = false; }

    public void tick() {
        if (!active) return;
        if (!access.valid() || access.carriedCount(key) != 0
                || (toCursor && access.countInSlot(staging, key) != moved)) { cancel(); return; }
        Source source = access.findSource(key); // Fresh index after every removal or server correction.
        if (source == null || moved >= target) { finish(); return; }
        if (!toCursor && (source.count() > target - moved || access.findDestination(key) < 0)) { finish(); return; }
        access.extract(source, key);
        int extracted = access.carriedCount(key);
        if (extracted <= 0) { cancel(); return; }

        int before = extracted;
        if (toCursor) {
            access.deposit(staging);
        } else {
            // A source can spill across existing partial stacks and then empty slots.
            for (int attempts = 0; attempts < 128 && access.carriedCount(key) > 0; attempts++) {
                int destination = access.findDestination(key);
                if (destination < 0) break;
                int count = access.carriedCount(key);
                access.deposit(destination);
                if (access.carriedCount(key) >= count) break;
            }
        }
        int remainder = access.carriedCount(key);
        if (remainder < 0) { cancel(); return; }
        moved += before - remainder;
        if (remainder > 0) access.returnRemainder(source.bundleSlot());
        if (access.carriedCount(key) != 0) { cancel(); return; } // Leave any unexpected cursor item visible.
        if (before == remainder || remainder > 0 || moved >= target) finish();
    }

    private void finish() {
        active = false;
        if (toCursor && moved > 0 && access.valid() && access.carriedCount(key) == 0
                && access.countInSlot(staging, key) == moved) access.pickup(staging);
    }
}
