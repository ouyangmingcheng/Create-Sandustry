package com.minechai.create_sandustry.content.kineticpress;

import com.minechai.create_sandustry.registry.ModBlockEntities;
import com.minechai.create_sandustry.registry.ModTags;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class KineticPressBlock extends Block implements IBE<KineticPressBlockEntity> {
    /** Visual projection of a valid structure. The authoritative flag lives in the block entity. */
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    /** Redstone gate and display state. Powered pauses processing/output and has visual priority over WORKING. */
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public KineticPressBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(WORKING, false)
                .setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WORKING, POWERED);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(ModTags.ItemTags.KINETIC_PRESS_INPUTS)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        KineticPressBlockEntity press = getBlockEntity(level, pos);
        if (press == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return KineticPressPlacementHandler.tryPlace(level, press, player, hand, hitResult)
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!player.isShiftKeyDown() || !player.getMainHandItem().isEmpty() || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (!level.mayInteract(player, pos) || !player.mayBuild()) {
                return InteractionResult.FAIL;
            }
            KineticPressBlockEntity press = getBlockEntity(level, pos);
            if (press == null) {
                return InteractionResult.PASS;
            }
            press.returnQueuedInputs(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide) {
            return;
        }
        updatePoweredState(level, pos, state);
        withBlockEntityDo(level, pos, KineticPressBlockEntity::requestStructureCheck);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide) {
            return;
        }
        updatePoweredState(level, pos, state);
        withBlockEntityDo(level, pos, KineticPressBlockEntity::requestStructureCheck);
    }

    private static void updatePoweredState(Level level, BlockPos pos, BlockState state) {
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<KineticPressBlockEntity> getBlockEntityClass() {
        return KineticPressBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends KineticPressBlockEntity> getBlockEntityType() {
        return ModBlockEntities.KINETIC_PRESS.get();
    }
}
