package com.minechai.create_sandustry.content.moonhop;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.minechai.create_sandustry.registry.ModDataComponents;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Pure state machine and output construction for moonhop pressing. */
public final class MoonhopPressLogic {
    public enum ResultType {
        PASS,
        UPDATE,
        COMPLETE,
        DESTROY
    }

    public record Result(ResultType type, MoonhopProcessState nextState) {
        private static final Result PASS = new Result(ResultType.PASS, null);
        private static final Result COMPLETE = new Result(ResultType.COMPLETE, null);
        private static final Result DESTROY = new Result(ResultType.DESTROY, null);

        public Result {
            if (type == ResultType.UPDATE) {
                Objects.requireNonNull(nextState, "UPDATE result requires a process state");
            }
        }

        public boolean startsProcessing() {
            return type != ResultType.PASS;
        }
    }

    public static boolean isMoonhopInput(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.MOONHOP.get()) || stack.is(ModItems.INCOMPLETE_MOONHOP_BOARD.get()));
    }

    public static Result evaluate(ItemStack stack, ResourceKey<Level> dimension, int pressY) {
        if (stack.isEmpty()) {
            return Result.PASS;
        }
        if (stack.is(ModItems.INCOMPLETE_MOONHOP_BOARD.get())) {
            return evaluateIncomplete(stack, pressY);
        }
        if (stack.is(ModItems.MOONHOP.get())) {
            return evaluateFirstPress(dimension, pressY);
        }
        return Result.PASS;
    }

    public static Result evaluateFirstPress(ResourceKey<Level> dimension, int pressY) {
        if (dimension.equals(Level.END)) {
            return Result.COMPLETE;
        }

        int target;
        if (dimension.equals(Level.OVERWORLD)) {
            target = MoonhopProcessState.OVERWORLD_TARGET;
        } else if (dimension.equals(Level.NETHER)) {
            target = MoonhopProcessState.NETHER_TARGET;
        } else {
            return Result.PASS;
        }

        return classify(target, pressY, List.of(pressY));
    }

    public static Result evaluateIncomplete(ItemStack stack, int pressY) {
        if (stack.getCount() != 1) {
            return Result.PASS;
        }

        MoonhopProcessState state = stack.get(ModDataComponents.MOONHOP_PROCESS_STATE.get());
        if (state == null || !state.isValid()) {
            return Result.PASS;
        }
        if (state.hasUsed(pressY)) {
            return Result.PASS;
        }

        long newProgress = (long) state.progress() + pressY;
        ArrayList<Integer> nextUsedY = new ArrayList<>(state.usedY());
        nextUsedY.add(pressY);
        return classify(state.target(), newProgress, nextUsedY);
    }

    private static Result classify(int target, long newProgress, List<Integer> usedY) {
        if (newProgress < 0L || newProgress > target) {
            return Result.DESTROY;
        }
        if (newProgress == target) {
            return Result.COMPLETE;
        }

        MoonhopProcessState nextState = new MoonhopProcessState(target, (int) newProgress, usedY);
        if (!nextState.isValid()) {
            // This is an internal consistency failure, not a gameplay loss condition.
            return Result.PASS;
        }
        return new Result(ResultType.UPDATE, nextState);
    }

    public static int processedCount(ItemStack input, boolean bulkPressing) {
        if (input.is(ModItems.INCOMPLETE_MOONHOP_BOARD.get())) {
            return input.getCount() == 1 ? 1 : 0;
        }
        if (input.is(ModItems.MOONHOP.get())) {
            return bulkPressing ? input.getCount() : Math.min(1, input.getCount());
        }
        return 0;
    }

    public static List<ItemStack> createOutputs(ItemStack input, Result result, int processedCount) {
        if (!result.startsProcessing() || result.type() == ResultType.DESTROY || processedCount <= 0) {
            return List.of();
        }

        int count = Math.min(processedCount, input.getCount());
        if (count <= 0) {
            return List.of();
        }

        ItemStack outputTemplate;
        if (result.type() == ResultType.COMPLETE) {
            outputTemplate = new ItemStack(ModItems.MOONHOP_BOARD.get());
        } else if (result.type() == ResultType.UPDATE) {
            outputTemplate = new ItemStack(ModItems.INCOMPLETE_MOONHOP_BOARD.get());
            outputTemplate.set(ModDataComponents.MOONHOP_PROCESS_STATE.get(), result.nextState());
        } else {
            return List.of();
        }

        return ItemHelper.multipliedOutput(input.copyWithCount(count), outputTemplate);
    }

    public static ItemStack createIncompleteStack(MoonhopProcessState state) {
        ItemStack stack = new ItemStack(ModItems.INCOMPLETE_MOONHOP_BOARD.get());
        stack.set(ModDataComponents.MOONHOP_PROCESS_STATE.get(), state);
        return stack;
    }

    private MoonhopPressLogic() {
    }
}
