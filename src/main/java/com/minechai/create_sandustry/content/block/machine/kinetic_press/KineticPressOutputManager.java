package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import java.util.List;
import java.util.UUID;

import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Server-only automatic delivery of the existing output cache. */
public final class KineticPressOutputManager {
    private static final String DROP_ID_TAG = "KineticPressDropId";
    private static final int RETRY_INTERVAL = 10;

    private final KineticPressBlockEntity blockEntity;
    private final KineticPressOutputHandler outputHandler;

    private int retryCooldown;
    private int nextOutputSlot;
    private ItemStack inFlightDrop = ItemStack.EMPTY;
    private UUID inFlightDropId;

    public KineticPressOutputManager(KineticPressBlockEntity blockEntity,
                                     KineticPressOutputHandler outputHandler) {
        this.blockEntity = blockEntity;
        this.outputHandler = outputHandler;
    }

    public void tick(boolean powered) {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        if (!inFlightDrop.isEmpty()) {
            resolvePendingDrop(level);
            return;
        }
        if (powered) {
            return;
        }
        if (retryCooldown > 0) {
            retryCooldown--;
            return;
        }

        boolean delivered = tryOutputOne(level);
        retryCooldown = delivered ? 0 : RETRY_INTERVAL;
    }

    private boolean tryOutputOne(Level level) {
        for (int offset = 0; offset < outputHandler.getSlots(); offset++) {
            int slot = (nextOutputSlot + offset) % outputHandler.getSlots();
            if (outputHandler.getStackInSlot(slot).isEmpty()) continue;
            if (tryOutputSlot(level, slot)) {
                nextOutputSlot = (slot + 1) % outputHandler.getSlots();
                return true;
            }
        }
        nextOutputSlot = (nextOutputSlot + 1) % outputHandler.getSlots();
        return false;
    }

    private boolean tryOutputSlot(Level level, int slot) {
        ItemStack candidate = outputHandler.getStackInSlot(slot).copy();
        if (candidate.isEmpty()) {
            return false;
        }

        BlockPos below = blockEntity.getBlockPos().below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.isAir()) {
            return dropOne(level, slot, candidate, below);
        }

        DirectBeltInputBehaviour direct =
                BlockEntityBehaviour.get(level, below, DirectBeltInputBehaviour.TYPE);
        if (direct != null && direct.canInsertFromSide(Direction.UP)
                && !direct.isOccupied(Direction.UP)) {
            return insertViaDirect(slot, candidate, direct);
        }

        IItemHandler handler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, below, Direction.UP);
        if (handler != null) {
            return insertViaHandler(slot, candidate, handler);
        }
        return false;
    }

    private boolean insertViaDirect(int slot, ItemStack candidate,
                                    DirectBeltInputBehaviour direct) {
        ItemStack simulatedRemainder = direct.handleInsertion(
                candidate.copy(), Direction.UP, true);
        int accepted = candidate.getCount() - simulatedRemainder.getCount();
        if (accepted <= 0) {
            return false;
        }

        ItemStack extracted = outputHandler.extractItem(slot, accepted, false);
        if (extracted.isEmpty()) {
            return false;
        }

        ItemStack actualRemainder = direct.handleInsertion(
                extracted.copy(), Direction.UP, false);
        int delivered = extracted.getCount() - actualRemainder.getCount();
        if (!actualRemainder.isEmpty()) {
            outputHandler.insertInternal(actualRemainder);
        }
        return delivered > 0;
    }

    private boolean insertViaHandler(int slot, ItemStack candidate, IItemHandler handler) {
        ItemStack simulatedRemainder = ItemHandlerHelper.insertItemStacked(
                handler, candidate.copy(), true);
        int accepted = candidate.getCount() - simulatedRemainder.getCount();
        if (accepted <= 0) {
            return false;
        }

        ItemStack extracted = outputHandler.extractItem(slot, accepted, false);
        if (extracted.isEmpty()) {
            return false;
        }

        ItemStack actualRemainder = ItemHandlerHelper.insertItemStacked(
                handler, extracted.copy(), false);
        int delivered = extracted.getCount() - actualRemainder.getCount();
        if (!actualRemainder.isEmpty()) {
            outputHandler.insertInternal(actualRemainder);
        }
        return delivered > 0;
    }

    private boolean dropOne(Level level, int slot, ItemStack candidate, BlockPos below) {
        ItemStack extracted = outputHandler.extractItem(slot, candidate.getCount(), false);
        if (extracted.isEmpty()) {
            return false;
        }

        UUID dropId = UUID.randomUUID();
        inFlightDrop = extracted.copy();
        inFlightDropId = dropId;
        blockEntity.setChanged();

        ItemEntity entity = new ItemEntity(level,
                below.getX() + 0.5D,
                below.getY() + 0.75D,
                below.getZ() + 0.5D,
                extracted.copy());
        entity.setDefaultPickUpDelay();
        entity.getPersistentData().putUUID(DROP_ID_TAG, dropId);

        if (!level.addFreshEntity(entity)) {
            outputHandler.insertInternal(extracted);
            clearInFlightDrop();
            return false;
        }

        clearInFlightDrop();
        return true;
    }

    private void resolvePendingDrop(Level level) {
        if (inFlightDrop.isEmpty()) {
            return;
        }

        BlockPos below = blockEntity.getBlockPos().below();
        List<ItemEntity> existing = level.getEntitiesOfClass(
                ItemEntity.class,
                new net.minecraft.world.phys.AABB(below).inflate(8.0D),
                entity -> inFlightDropId != null
                        && entity.getPersistentData().contains(DROP_ID_TAG)
                        && inFlightDropId.equals(entity.getPersistentData().getUUID(DROP_ID_TAG)));

        if (existing.isEmpty()) {
            // The crash/load happened before a matching entity was committed;
            // restore the reserved stack instead of spawning a duplicate.
            ItemStack remainder = outputHandler.insertInternal(inFlightDrop.copy());
            if (!remainder.isEmpty()) {
                inFlightDrop = remainder;
                blockEntity.setChanged();
                return;
            }
        }
        clearInFlightDrop();
    }

    private void clearInFlightDrop() {
        inFlightDrop = ItemStack.EMPTY;
        inFlightDropId = null;
        blockEntity.setChanged();
    }

    public void write(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("NextOutputSlot", nextOutputSlot);
        tag.put("InFlightDropItem", inFlightDrop.saveOptional(provider));
        if (inFlightDropId != null) {
            tag.putUUID("InFlightDropId", inFlightDropId);
        }
    }

    public void read(CompoundTag tag, HolderLookup.Provider provider) {
        nextOutputSlot = Math.floorMod(tag.getInt("NextOutputSlot"), outputHandler.getSlots());
        inFlightDrop = ItemStack.parseOptional(provider, tag.getCompound("InFlightDropItem"));
        inFlightDropId = tag.hasUUID("InFlightDropId") ? tag.getUUID("InFlightDropId") : null;
    }

    public ItemStack resolveForRemoval() {
        if (blockEntity.getLevel() != null && !inFlightDrop.isEmpty()) {
            resolvePendingDrop(blockEntity.getLevel());
        }
        ItemStack remaining = inFlightDrop.copy();
        clearInFlightDrop();
        return remaining;
    }
}
