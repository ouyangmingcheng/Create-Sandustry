package com.minechai.create_sandustry.content.block;

import com.minechai.create_sandustry.content.block.entity.SandMasterSwordPedestalBlockEntity;
import com.minechai.create_sandustry.content.sandheart.SandSwordPullManager;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 沙噬剑座：一座可放置、可插取完整沙噬剑的古老砂岩祭坛。
 *
 * <p>方块模型只包含祭坛本体；中央竖直插着的剑由 {@code SandMasterSwordPedestalRenderer}
 * 作为独立物品模型绘制。{@link #HAS_SAND_SWORD} 只用于让插有沙金剑的剑座加入原版避雷针 POI，
 * 不参与模型旋转，也不会改变插剑时记录的剑面朝向。</p>
 */
public class SandMasterSwordPedestalBlock extends BaseEntityBlock {
    public static final MapCodec<SandMasterSwordPedestalBlock> CODEC =
            simpleCodec(SandMasterSwordPedestalBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** 只有插有沙金剑的剑座会加入原版避雷针 POI。 */
    public static final BooleanProperty HAS_SAND_SWORD = BooleanProperty.create("has_sand_sword");

    private static final VoxelShape COLLISION_SHAPE = Shapes.or(
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 5.0D, 13.0D),
            Block.box(5.0D, 5.0D, 5.0D, 11.0D, 5.35D, 11.0D));

    /** 选择框向剑柄方向延伸，保证插剑时仍能直接点选整个祭坛。 */
    private static final VoxelShape SELECTION_SHAPE = Shapes.or(
            COLLISION_SHAPE,
            Block.box(6.0D, 5.0D, 6.0D, 10.0D, 17.0D, 10.0D));

    public SandMasterSwordPedestalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_SAND_SWORD, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_SAND_SWORD);
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

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(HAS_SAND_SWORD)) {
            return;
        }
        // 与 LightningRodBlock#animateTick 相同的雷暴电火花表现。
        if (level.isThundering()
                && (long) level.random.nextInt(200) <= level.getGameTime() % 200L
                && pos.getY() == level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) - 1) {
            ParticleUtils.spawnParticlesAlongAxis(Direction.Axis.Y, level, pos, 0.125D,
                    ParticleTypes.ELECTRIC_SPARK, UniformInt.of(1, 2));
        }
    }
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SandMasterSwordPedestalBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (SandSwordPullManager.isInteractionLocked(player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
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

    /** 原版 LightningBolt 完成命中判定后调用；只修改 BlockEntity，不替换方块朝向。 */
    public void onLightningStrike(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof SandMasterSwordPedestalBlockEntity pedestal) {
            pedestal.transformToUnowned();
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (SandSwordPullManager.isInteractionLocked(player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!(level.getBlockEntity(pos) instanceof SandMasterSwordPedestalBlockEntity pedestal)
                || !pedestal.hasSword()) {
            return InteractionResult.PASS;
        }

        if (pedestal.isUnowned()) {
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                SandSwordPullManager.tryStart(serverPlayer, pos, pedestal);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
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
            boolean unowned = pedestal.isUnowned();
            if (level instanceof ServerLevel serverLevel) {
                SandSwordPullManager.cancelForPedestal(serverLevel, pos);
            }
            ItemStack sword = pedestal.clearSword();
            // 无主剑座被破坏时只掉落剑座本体，不返还无主沙噬剑。
            if (!unowned && !sword.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D, sword);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}