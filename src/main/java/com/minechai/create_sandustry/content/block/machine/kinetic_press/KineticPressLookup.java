package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Small index of loaded bases and their bound inputs, shared by both logical sides. */
public final class KineticPressLookup {
    // Index values must not hold a Level or a strong BE reference: either would
    // keep the weak key alive across world changes and integrated-server exits.
    private static final Map<Level, Index> LEVELS = new WeakHashMap<>();

    private KineticPressLookup() {
    }

    public static synchronized void update(KineticPressBlockEntity press) {
        Level level = press.getLevel();
        if (level == null || press.isRemoved()) return;
        Index index = LEVELS.computeIfAbsent(level, unused -> new Index());
        BlockPos basePos = press.getBlockPos();
        Entry entry = index.bases.get(basePos);
        if (entry == null || entry.press.get() != press) {
            if (entry != null) removeEntry(index, entry);
            entry = new Entry(press);
            index.bases.put(entry.basePos, entry);
        }
        BlockPos inputPos = press.getBoundInputPos();
        if (entry.inputPos != null && !entry.inputPos.equals(inputPos)) {
            removeInputEntry(index, entry);
        }
        entry.inputPos = inputPos == null ? null : inputPos.immutable();
        if (entry.inputPos != null) {
            List<Entry> candidates = index.inputs.computeIfAbsent(entry.inputPos, unused -> new ArrayList<>());
            if (!candidates.contains(entry)) candidates.add(entry);
        }
    }

    public static synchronized void remove(KineticPressBlockEntity press) {
        Level level = press.getLevel();
        Index index = LEVELS.get(level);
        if (index == null) return;
        Entry entry = index.bases.get(press.getBlockPos());
        if (entry != null && entry.press.get() == press) removeEntry(index, entry);
        if (index.bases.isEmpty()) LEVELS.remove(level);
    }

    /** One indexed lookup replaces the former 21 block-entity world lookups. */
    public static synchronized KineticPressBlockEntity findForInput(Level level, BlockPos inputPos) {
        Index index = LEVELS.get(level);
        if (index == null) return null;
        List<Entry> candidates = index.inputs.get(inputPos);
        if (candidates == null) return null;
        KineticPressBlockEntity nearest = null;
        // An obstructed lower base can retain the same binding as a newer base
        // above it. Preserve the original downward scan's nearest-base priority,
        // independent of registration order, and remove only each owner's entry.
        for (Entry entry : List.copyOf(candidates)) {
            KineticPressBlockEntity press = resolve(level, entry);
            if (press == null) {
                removeEntry(index, entry);
            } else if (!press.isBoundToInput(inputPos) || !press.hasBoundInput()) {
                removeInputEntry(index, entry);
            } else if (nearest == null || press.getBlockPos().getY() > nearest.getBlockPos().getY()) {
                // Keep invalid owners too: their chute must hold its item until
                // structure recovery, rather than fall back to dropping it.
                nearest = press;
            }
        }
        return nearest;
    }

    /** No world BE lookups; include the two lowest gap blocks as well as inputs. */
    public static void notifyColumnChange(Level level, BlockPos changedPos) {
        List<KineticPressBlockEntity> affected;
        synchronized (KineticPressLookup.class) {
            Index index = LEVELS.get(level);
            if (index == null) return;
            affected = new ArrayList<>();
            for (int offset = 0; offset <= KineticPressStructure.MAX_WORKING_HEIGHT; offset++) {
                Entry entry = index.bases.get(changedPos.below(offset));
                if (entry == null) continue;
                KineticPressBlockEntity press = resolve(level, entry);
                if (press == null) {
                    removeEntry(index, entry);
                } else if (offset <= press.getWorkingHeight()) {
                    affected.add(press);
                }
            }
        }
        // Validation can update bindings, so call it outside the map iteration.
        for (KineticPressBlockEntity press : affected) press.requestStructureCheck();
    }

    private static KineticPressBlockEntity resolve(Level level, Entry entry) {
        if (entry == null) return null;
        KineticPressBlockEntity press = entry.press.get();
        return press != null && !press.isRemoved() && press.getLevel() == level
                && level.isLoaded(entry.basePos) ? press : null;
    }

    private static void removeEntry(Index index, Entry entry) {
        index.bases.remove(entry.basePos, entry);
        removeInputEntry(index, entry);
    }

    private static void removeInputEntry(Index index, Entry entry) {
        if (entry.inputPos == null) return;
        List<Entry> candidates = index.inputs.get(entry.inputPos);
        if (candidates != null) {
            candidates.remove(entry);
            if (candidates.isEmpty()) index.inputs.remove(entry.inputPos);
        }
        entry.inputPos = null;
    }

    private static final class Index {
        final Map<BlockPos, Entry> bases = new HashMap<>();
        final Map<BlockPos, List<Entry>> inputs = new HashMap<>();
    }

    private static final class Entry {
        final WeakReference<KineticPressBlockEntity> press;
        final BlockPos basePos;
        BlockPos inputPos;

        Entry(KineticPressBlockEntity press) {
            this.press = new WeakReference<>(press);
            this.basePos = press.getBlockPos().immutable();
        }
    }
}
