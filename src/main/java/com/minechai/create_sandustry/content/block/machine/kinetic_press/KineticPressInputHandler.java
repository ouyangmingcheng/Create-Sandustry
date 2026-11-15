package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandler;

/** Input-only capability view. One slot represents exactly one queued item. */
public final class KineticPressInputHandler implements IItemHandler, INBTSerializable<CompoundTag> {
    public static final int MAX_ITEMS = 16;

    private final KineticPressBlockEntity blockEntity;
    private final NonNullList<ItemStack> slots = NonNullList.withSize(MAX_ITEMS, ItemStack.EMPTY);

    public KineticPressInputHandler(KineticPressBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public int getSlots() {
        return MAX_ITEMS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlot(slot);
        return slots.get(slot).copy();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlot(slot);
        if (stack.isEmpty() || !blockEntity.canAcceptInput()
                || !blockEntity.getProcessor().matchesInput(stack)) {
            return stack;
        }
        if (!slots.get(slot).isEmpty() || countItems() >= MAX_ITEMS) {
            return stack;
        }

        if (!simulate) {
            slots.set(slot, stack.copyWithCount(1));
            blockEntity.onInputInserted();
        }
        return stack.getCount() <= 1 ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - 1);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        validateSlot(slot);
        return !stack.isEmpty() && blockEntity.canAcceptInput() && slots.get(slot).isEmpty()
                && blockEntity.getProcessor().matchesInput(stack);
    }

    public ItemStack getInternal(int slot) {
        validateSlot(slot);
        return slots.get(slot);
    }

    public ItemStack removeOne(int slot) {
        validateSlot(slot);
        ItemStack stack = slots.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        slots.set(slot, ItemStack.EMPTY);
        return stack;
    }

    public int countItems() {
        int count = 0;
        for (ItemStack stack : slots) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public boolean isEmpty() {
        return countItems() == 0;
    }

    public void clear() {
        for (int i = 0; i < slots.size(); i++) {
            slots.set(i, ItemStack.EMPTY);
        }
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= MAX_ITEMS) {
            throw new RuntimeException("Input slot " + slot + " outside [0," + MAX_ITEMS + ")");
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        ListTag items = new ListTag();
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = slots.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("Slot", i);
            itemTag.put("Item", stack.saveOptional(provider));
            items.add(itemTag);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("Items", items);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        clear();
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot >= 0 && slot < slots.size()) {
                slots.set(slot, ItemStack.parseOptional(provider, itemTag.getCompound("Item")));
            }
        }
    }
}
