package com.minechai.create_candustry.block;

import com.minechai.create_candustry.block.entity.SandMasterSwordPedestalBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 沙噬剑座：一座可放置、可插取完整沙噬剑的古老砂岩祭坛。
 *
 * <p>方块模型只包含祭坛本体；中央竖直插着的剑由 {@code SandMasterSwordPedestalRenderer}
 * 作为独立物品模型绘制，因此插剑与取剑不会改变方块状态，也不会破坏剑本身的附魔和命名数据。</p>
 */
public class SandMasterSwordPedestalBlock extends BaseEntityBlock {
    public static final MapCodec<SandMasterSwordPedestalBlock> CODEC =
            simpleCodec(SandMasterSwordPedestalBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape COLLISION_SHAPE = Shapes.or(
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 5.0D, 13.0D),
            Block.box(5.0D, 5.0D, 5.0D, 11.0D, 5.35D, 11.0D));

    /** 选择框向剑柄方向延伸，保证插剑时仍能直接点选整个祭坛。 */
    private static final VoxelShape SELECTION_SHAPE = Shapes.or(
            COLLISION_SHAPE,
            Block.box(6.0D, 5.0D, 6.0D, 10.0D, 17.0D, 10.0D));

    public SandMasterSwordPedestalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SELECTION_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SandMasterSwordPedestalBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof SandMasterSwordPedestalBlockEntity pedestal)
                || pedestal.hasSword()
                || !SandMasterSwordPedestalBlockEntity.isAcceptedSword(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            pedestal.setSword(stack.copyWithCount(1), player.getDirection().getOpposite());
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.9F, 0.78F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof SandMasterSwordPedestalBlockEntity pedestal)
                || !pedestal.hasSword()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            ItemStack removed = pedestal.removeSword();
            if (!player.getInventory().add(removed)) {
                player.drop(removed, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 1.15F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof SandMasterSwordPedestalBlockEntity pedestal) {
            ItemStack sword = pedestal.clearSword();
            if (!sword.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D, sword);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}