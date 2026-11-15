package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import com.minechai.create_sandustry.registry.ModTags;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** Validates the base-plus-input structure without taking ownership of block data. */
public final class KineticPressStructure {
    public static final int MIN_WORKING_HEIGHT = 3;
    public static final int MAX_WORKING_HEIGHT = 23;
    public static final int DEFAULT_WORKING_HEIGHT = 8;

    private KineticPressStructure() {
    }

    public static BlockPos getInputPosition(BlockPos basePos, int workingHeight) {
        return basePos.above(workingHeight);
    }

    public static boolean isStructureValid(LevelReader level, BlockPos basePos, int workingHeight) {
        return validate(level, basePos, workingHeight).valid();
    }

    public static ValidationResult validate(LevelReader level, BlockPos basePos, int workingHeight) {
        if (workingHeight < MIN_WORKING_HEIGHT || workingHeight > MAX_WORKING_HEIGHT) {
            return ValidationResult.failure(FailureReason.INVALID_HEIGHT, null, null);
        }

        BlockPos inputPos = getInputPosition(basePos, workingHeight);
        if (level.isOutsideBuildHeight(inputPos.getY())) {
            return ValidationResult.failure(FailureReason.OUT_OF_WORLD, inputPos, null);
        }

        BlockState inputState = level.getBlockState(inputPos);
        if (!inputState.is(ModTags.BlockTags.KINETIC_PRESS_INPUTS)) {
            return ValidationResult.failure(FailureReason.MISSING_INPUT, inputPos, inputPos);
        }

        for (int offset = 1; offset < workingHeight; offset++) {
            BlockPos gapPos = basePos.above(offset);
            if (!level.getBlockState(gapPos).isAir()) {
                return ValidationResult.failure(FailureReason.BLOCKED_GAP, inputPos, gapPos);
            }
        }

        return ValidationResult.success(inputPos);
    }

    public enum FailureReason {
        NONE,
        INVALID_HEIGHT,
        OUT_OF_WORLD,
        MISSING_INPUT,
        BLOCKED_GAP
    }

    public record ValidationResult(boolean valid, FailureReason reason, BlockPos inputPosition,
                                   BlockPos blockingPosition) {
        public static ValidationResult success(BlockPos inputPosition) {
            return new ValidationResult(true, FailureReason.NONE, inputPosition, null);
        }

        public static ValidationResult failure(FailureReason reason, BlockPos inputPosition,
                                               BlockPos blockingPosition) {
            return new ValidationResult(false, reason, inputPosition, blockingPosition);
        }
    }
}