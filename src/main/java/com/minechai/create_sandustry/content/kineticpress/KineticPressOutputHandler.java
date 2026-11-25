package com.minechai.create_sandustry.content.kineticpress;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Extract-only output cache. Internal processing uses insertInternal(). */
public final class KineticPressOutputHandler extends ItemStackHandler {
    public static final int SLOT_COUNT = 9;

    private final KineticPressBlockEntity blockEntity;

    public KineticPressOutputHandler(KineticPressBlockEntity blockEntity) {
        super(SLOT_COUNT);
        this.blockEntity = blockEntity;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return blockEntity.isPowered() ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
    }

    public ItemStack extractInternal(int slot, int amount) {
        return super.extractItem(slot, amount, false);
    }

    @Override
    protected void onContentsChanged(int slot) {
        blockEntity.onOutputChanged();
    }

    public ItemStack insertInternal(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < getSlots() && !remaining.isEmpty(); i++) {
            ItemStack existing = stacks.get(i);
            int slotLimit = Math.min(getSlotLimit(i), remaining.getMaxStackSize());
            int moved = 0;
            if (existing.isEmpty()) {
                moved = Math.min(remaining.getCount(), slotLimit);
                stacks.set(i, remaining.copyWithCount(moved));
            } else if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                int space = slotLimit - existing.getCount();
                moved = Math.min(remaining.getCount(), Math.max(0, space));
                if (moved > 0) {
                    existing.grow(moved);
                }
            }
            if (moved > 0) {
                remaining.shrink(moved);
                onContentsChanged(i);
            }
        }
        return remaining;
    }

    public boolean canAcceptAll(List<ItemStack> stacks) {
        ItemStackHandler simulation = new ItemStackHandler(SLOT_COUNT);
        for (int i = 0; i < getSlots(); i++) {
            simulation.setStackInSlot(i, getStackInSlot(i).copy());
        }
        for (ItemStack stack : stacks) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(simulation, stack.copy(), false);
            if (!remainder.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
