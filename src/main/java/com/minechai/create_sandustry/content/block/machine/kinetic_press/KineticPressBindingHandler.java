package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModTags;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Minimal bound-chute movement logic; unrelated Create chutes are untouched. */
public final class KineticPressBindingHandler {
    private KineticPressBindingHandler() {
    }

    public static MoveResult moveBoundInput(KineticPressBlockEntity press, int newHeight, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return MoveResult.failure(Failure.NO_PERMISSION, null);
        }
        Level level = press.getLevel();
        BlockPos basePos = press.getBlockPos();
        BlockPos oldPos = press.getBoundInputPos();
        if (level == null || oldPos == null) {
            return MoveResult.failure(Failure.MISSING_INPUT, oldPos);
        }

        BlockPos newPos = KineticPressStructure.getInputPosition(basePos, newHeight);
        if (oldPos.equals(newPos)) {
            return MoveResult.success(newPos);
        }
        if (level.isOutsideBuildHeight(newPos.getY())) {
            return MoveResult.failure(Failure.OUT_OF_WORLD, newPos);
        }

        BlockState oldState = level.getBlockState(oldPos);
        if (!oldState.is(ModTags.BlockTags.KINETIC_PRESS_INPUTS)) {
            return MoveResult.failure(Failure.MISSING_INPUT, oldPos);
        }
        if (!level.getBlockState(newPos).isAir()) {
            return MoveResult.failure(Failure.TARGET_OCCUPIED, newPos);
        }

        for (int offset = 1; offset < newHeight; offset++) {
            BlockPos pathPos = basePos.above(offset);
            if (pathPos.equals(oldPos)) {
                continue;
            }
            if (!level.getBlockState(pathPos).isAir()) {
                return MoveResult.failure(Failure.PATH_BLOCKED, pathPos);
            }
        }
        if (!oldState.canSurvive(level, newPos)) {
            return MoveResult.failure(Failure.CANNOT_SURVIVE, newPos);
        }

        GameType gameType = serverPlayer.gameMode.getGameModeForPlayer();
        if (!level.mayInteract(player, oldPos)
                || !level.mayInteract(player, newPos)
                || player.blockActionRestricted(level, oldPos, gameType)
                || !player.mayUseItemAt(newPos, Direction.DOWN, ItemStack.EMPTY)) {
            return MoveResult.failure(Failure.NO_PERMISSION, newPos);
        }

        BlockEntity oldBlockEntity = level.getBlockEntity(oldPos);
        CompoundTag savedData = oldBlockEntity == null
                ? new CompoundTag()
                : oldBlockEntity.saveWithFullMetadata(level.registryAccess());

        // ChuteBlockEntity.destroy() drops its held item. Ownership now belongs
        // to savedData until the destination (or rollback source) loads it.
        clearTransferredContents(oldBlockEntity);
        if (!level.removeBlock(oldPos, false)) {
            restore(level, oldPos, oldState, savedData);
            return MoveResult.failure(Failure.MOVE_FAILED, oldPos);
        }

        if (!level.setBlock(newPos, oldState, Block.UPDATE_ALL)) {
            restore(level, oldPos, oldState, savedData);
            return MoveResult.failure(Failure.MOVE_FAILED, newPos);
        }

        BlockEntity newBlockEntity = level.getBlockEntity(newPos);
        if (newBlockEntity == null) {
            level.removeBlock(newPos, false);
            restore(level, oldPos, oldState, savedData);
            return MoveResult.failure(Failure.MOVE_FAILED, newPos);
        }

        try {
            if (!savedData.isEmpty()) {
                newBlockEntity.loadWithComponents(savedData, level.registryAccess());
            }
            newBlockEntity.setChanged();
            level.sendBlockUpdated(newPos, oldState, oldState, Block.UPDATE_CLIENTS);
        } catch (RuntimeException exception) {
            CreateSandustry.LOGGER.error("Cannot restore moved Kinetic Press input at {}; rolling back to {}",
                    newPos, oldPos, exception);
            // A partial load may already have populated the new chute. Its
            // removal must not drop a second copy before restoring savedData.
            clearTransferredContents(newBlockEntity);
            level.removeBlock(newPos, false);
            restore(level, oldPos, oldState, savedData);
            return MoveResult.failure(Failure.MOVE_FAILED, newPos);
        }

        press.bindInput(newPos, level.getBlockState(newPos));
        press.requestStructureCheck();
        return MoveResult.success(newPos);
    }

    private static void clearTransferredContents(BlockEntity blockEntity) {
        if (blockEntity instanceof ChuteBlockEntity chute) {
            chute.clearContent();
        }
    }

    private static void restore(Level level, BlockPos pos, BlockState state, CompoundTag savedData) {
        if (!level.setBlock(pos, state, Block.UPDATE_ALL)
                && !level.getBlockState(pos).equals(state)) {
            throw new IllegalStateException("Cannot restore Kinetic Press input at " + pos);
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!savedData.isEmpty()) {
            if (blockEntity == null) {
                throw new IllegalStateException("Missing block entity while restoring Kinetic Press input at " + pos);
            }
            blockEntity.loadWithComponents(savedData, level.registryAccess());
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public enum Failure {
        NONE,
        NO_PERMISSION,
        MISSING_INPUT,
        OUT_OF_WORLD,
        TARGET_OCCUPIED,
        PATH_BLOCKED,
        CANNOT_SURVIVE,
        MOVE_FAILED
    }

    public record MoveResult(boolean success, Failure failure, BlockPos pos) {
        public static MoveResult success(BlockPos pos) {
            return new MoveResult(true, Failure.NONE, pos);
        }

        public static MoveResult failure(Failure failure, BlockPos pos) {
            return new MoveResult(false, failure, pos);
        }
    }
}
