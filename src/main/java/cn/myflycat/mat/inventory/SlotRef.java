package cn.myflycat.mat.inventory;

import org.graalvm.polyglot.HostAccess;

public final class SlotRef {
    private final String container;
    private final int slot;
    private final String id;
    private final int count;

    public SlotRef(String container, int slot, String id, int count) {
        this.container = container;
        this.slot = slot;
        this.id = id;
        this.count = count;
    }

    @HostAccess.Export
    public String getContainer() { return container; }

    @HostAccess.Export
    public int getSlot() { return slot; }

    @HostAccess.Export
    public String getId() { return id; }

    @HostAccess.Export
    public int getCount() { return count; }

    @HostAccess.Export
    public boolean isEmpty() { return count == 0; }

    @HostAccess.Export
    @Override
    public String toString() {
        return container + "[" + slot + "]=" + id + "x" + count;
    }
}
