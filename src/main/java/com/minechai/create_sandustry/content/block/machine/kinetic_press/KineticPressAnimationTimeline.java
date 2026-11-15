package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.item.ItemStack;

/**
 * Display-only clock for authoritative task snapshots. It has no world or client
 * renderer dependency, and can never change inventories, task progress or FE.
 */
public final class KineticPressAnimationTimeline {
    public static final int PREDICTION_TICKS = 4;

    private final Map<Integer, State> states = new LinkedHashMap<>();
    private long latestRevision = Long.MIN_VALUE;
    private boolean advancing;

    public void accept(List<KineticPressAnimationSnapshot> snapshots, long revision, boolean advancing) {
        if (revision < latestRevision) return;
        latestRevision = revision;
        this.advancing = advancing;
        Set<Integer> present = new HashSet<>();
        for (KineticPressAnimationSnapshot snapshot : snapshots) {
            if (snapshot.totalTime() <= 0 || snapshot.stack().isEmpty()) continue;
            present.add(snapshot.id());
            State state = states.get(snapshot.id());
            if (state == null) {
                state = new State(snapshot);
                states.put(snapshot.id(), state);
            } else {
                state.accept(snapshot);
            }
            if (!advancing) state.hold();
        }
        for (Map.Entry<Integer, State> entry : states.entrySet()) {
            if (!present.contains(entry.getKey())) entry.getValue().finishing = true;
        }
    }

    /** Advances at most one predicted tick; a missing task gets one terminal render tick. */
    public void tick() {
        Iterator<State> iterator = states.values().iterator();
        while (iterator.hasNext()) {
            State state = iterator.next();
            if (state.finishing) {
                if (state.finishTickRendered) {
                    iterator.remove();
                } else {
                    state.previousProgress = state.currentProgress;
                    state.currentProgress = state.totalTime;
                    state.finishTickRendered = true;
                }
                continue;
            }
            state.previousProgress = state.currentProgress;
            if (advancing) {
                double predictionLimit = Math.min(state.totalTime - 1.0D,
                        state.serverProgress + (double) PREDICTION_TICKS);
                state.currentProgress = Math.max(state.currentProgress,
                        Math.min(state.currentProgress + 1.0D, predictionLimit));
            }
        }
    }

    public List<KineticPressAnimationRenderState> renderStates() {
        List<KineticPressAnimationRenderState> result = new ArrayList<>(states.size());
        for (State state : states.values()) {
            result.add(new KineticPressAnimationRenderState(state.id, state.stack, state.totalTime,
                    state.workingHeight, state.previousProgress, state.currentProgress));
        }
        return List.copyOf(result);
    }

    public boolean isEmpty() {
        return states.isEmpty();
    }

    public void clear() {
        states.clear();
        latestRevision = Long.MIN_VALUE;
        advancing = false;
    }

    private static final class State {
        private final int id;
        private final ItemStack stack;
        private final int totalTime;
        private final int workingHeight;
        private int serverProgress;
        private double previousProgress;
        private double currentProgress;
        private boolean finishing;
        private boolean finishTickRendered;

        private State(KineticPressAnimationSnapshot snapshot) {
            id = snapshot.id();
            stack = snapshot.stack().copy();
            totalTime = snapshot.totalTime();
            workingHeight = snapshot.workingHeight();
            serverProgress = Math.clamp(snapshot.progress(), 0, totalTime);
            previousProgress = currentProgress = serverProgress;
        }

        private void accept(KineticPressAnimationSnapshot snapshot) {
            finishing = false;
            finishTickRendered = false;
            int progress = Math.clamp(snapshot.progress(), 0, totalTime);
            serverProgress = Math.max(serverProgress, progress);
            if (progress > currentProgress) {
                previousProgress = currentProgress;
                currentProgress = progress;
            }
        }

        private void hold() {
            previousProgress = currentProgress;
        }
    }
}
