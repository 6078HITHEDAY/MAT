package cn.myflycat.mat.client.api;

import cn.myflycat.mat.client.inventory.InventoryAccess;
import cn.myflycat.mat.inventory.ItemQuery;
import cn.myflycat.mat.inventory.SlotRef;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.util.List;

public final class InventoryBindings {
    private final InventoryAccess access = new InventoryAccess();

    @HostAccess.Export
    public List<SlotRef> slots() {
        return access.slots();
    }

    @HostAccess.Export
    public List<SlotRef> find(Value query) {
        return access.find(ItemQuery.fromValue(query));
    }

    @HostAccess.Export
    public SlotRef hand() {
        return access.hand();
    }

    @HostAccess.Export
    public boolean selectHotbar(int index) {
        return access.selectHotbar(index);
    }

    @HostAccess.Export
    public boolean drop(SlotRef ref) {
        return access.drop(ref);
    }

    @HostAccess.Export
    public boolean quickMove(SlotRef ref) {
        return access.quickMove(ref);
    }

    @HostAccess.Export
    public boolean swap(int hotbarSlot, SlotRef target) {
        return access.swap(hotbarSlot, target);
    }
}
