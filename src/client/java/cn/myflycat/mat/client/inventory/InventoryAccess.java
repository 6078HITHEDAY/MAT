package cn.myflycat.mat.client.inventory;

import cn.myflycat.mat.client.api.ClientThread;
import cn.myflycat.mat.inventory.ItemQuery;
import cn.myflycat.mat.inventory.SlotRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class InventoryAccess {
    public List<SlotRef> slots() {
        return ClientThread.runSync(this::collectSlots);
    }

    public List<SlotRef> find(ItemQuery query) {
        return ClientThread.runSync(() -> {
            List<SlotRef> all = collectSlots();
            if (query == null || query.isEmpty()) return all;
            List<SlotRef> out = new ArrayList<>();
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            for (int i = 0; i < all.size(); i++) {
                SlotRef ref = all.get(i);
                if (matches(ref, query, stackForRef(p, ref))) out.add(ref);
            }
            return out;
        });
    }

    public SlotRef hand() {
        return ClientThread.runSync(() -> {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            if (p == null) return null;
            PlayerInventory inv = p.getInventory();
            return slotRef("hotbar", inv.selectedSlot, inv.getStack(inv.selectedSlot));
        });
    }

    public boolean selectHotbar(int index) {
        if (index < 0 || index > 8) return false;
        return ClientThread.runSync(() -> {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            if (p == null) return false;
            p.getInventory().selectedSlot = index;
            p.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(index));
            return true;
        });
    }

    public boolean drop(SlotRef ref) {
        return clickSlot(ref, 1, SlotActionType.THROW);
    }

    public boolean quickMove(SlotRef ref) {
        return clickSlot(ref, 0, SlotActionType.QUICK_MOVE);
    }

    public boolean swap(int hotbarSlot, SlotRef target) {
        if (hotbarSlot < 0 || hotbarSlot > 8) return false;
        return clickSlot(target, hotbarSlot, SlotActionType.SWAP);
    }

    private boolean clickSlot(SlotRef ref, int button, SlotActionType type) {
        return ClientThread.runSync(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            ClientPlayerEntity p = mc.player;
            if (p == null || mc.interactionManager == null) return false;
            if (p.currentScreenHandler != p.playerScreenHandler) return false;
            int sslot = screenSlot(ref);
            if (sslot < 0) return false;
            mc.interactionManager.clickSlot(p.currentScreenHandler.syncId, sslot, button, type, p);
            return true;
        });
    }

    private static int screenSlot(SlotRef ref) {
        if (ref == null) return -1;
        int s = ref.getSlot();
        return switch (ref.getContainer()) {
            case "hotbar" -> (s >= 0 && s <= 8) ? 36 + s : -1;
            case "inventory" -> (s >= 0 && s <= 26) ? 9 + s : -1;
            case "armor" -> (s >= 0 && s <= 3) ? 5 + s : -1;
            case "offhand" -> s == 0 ? 45 : -1;
            default -> -1;
        };
    }

    private List<SlotRef> collectSlots() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return List.of();
        PlayerInventory inv = p.getInventory();
        List<SlotRef> list = new ArrayList<>(46);
        for (int i = 0; i < 9; i++) {
            list.add(slotRef("hotbar", i, inv.getStack(i)));
        }
        for (int i = 9; i < 36; i++) {
            list.add(slotRef("inventory", i - 9, inv.getStack(i)));
        }
        for (int i = 0; i < inv.armor.size(); i++) {
            list.add(slotRef("armor", i, inv.armor.get(i)));
        }
        list.add(slotRef("offhand", 0, inv.offHand.get(0)));
        return list;
    }

    private static SlotRef slotRef(String container, int slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new SlotRef(container, slot, "minecraft:air", 0);
        }
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return new SlotRef(container, slot, id.toString(), stack.getCount());
    }

    private static ItemStack stackForRef(ClientPlayerEntity p, SlotRef ref) {
        if (p == null) return ItemStack.EMPTY;
        PlayerInventory inv = p.getInventory();
        return switch (ref.getContainer()) {
            case "hotbar" -> inv.getStack(ref.getSlot());
            case "inventory" -> inv.getStack(ref.getSlot() + 9);
            case "armor" -> ref.getSlot() < inv.armor.size() ? inv.armor.get(ref.getSlot()) : ItemStack.EMPTY;
            case "offhand" -> inv.offHand.get(0);
            default -> ItemStack.EMPTY;
        };
    }

    private static boolean matches(SlotRef ref, ItemQuery q, ItemStack stack) {
        if (q.container != null && !q.container.equals(ref.getContainer())) return false;
        if (q.id != null && !q.id.equals(ref.getId())) return false;
        if (q.minCount != null && ref.getCount() < q.minCount) return false;
        if (q.tag != null) {
            if (stack == null || stack.isEmpty()) return false;
            Identifier tagId = Identifier.tryParse(q.tag);
            if (tagId == null) return false;
            TagKey<net.minecraft.item.Item> tagKey = TagKey.of(RegistryKeys.ITEM, tagId);
            RegistryEntry<net.minecraft.item.Item> entry = Registries.ITEM.getEntry(stack.getItem());
            if (!entry.isIn(tagKey)) return false;
        }
        return true;
    }
}
