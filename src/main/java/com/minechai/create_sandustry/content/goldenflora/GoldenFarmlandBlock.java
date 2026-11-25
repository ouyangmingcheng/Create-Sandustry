package com.minechai.create_sandustry.content.goldenflora;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.FarmlandWaterManager;

/**
 * 黄金耕地：保留原版耕地的湿润、踩踏和退化规则，但退化目标改为黄金泥土。
 * 手持黄金胡萝卜右键时，会在上方种植黄金胡萝卜作物。
 */
public final class GoldenFarmlandBlock extends FarmBlock {
    public static final MapCodec<FarmBlock> CODEC = simpleCodec(GoldenFarmlandBlock::new);

    public GoldenFarmlandBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<FarmBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return !this.defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos())
                ? ModBlocks.GOLDEN_DIRT.get().defaultBlockState()
                : super.getStateForPlacement(context);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            turnToGoldenDirt(null, state, level, pos);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int moisture = state.getValue(MOISTURE);
        if (!isNearWater(level, pos) && !level.isRainingAt(pos.above())) {
            if (moisture > 0) {
                level.setBlock(pos, state.setValue(MOISTURE, moisture - 1), 2);
            } else if (!shouldMaintainFarmland(level, pos)) {
                turnToGoldenDirt(null, state, level, pos);
            }
        } else if (moisture < MAX_MOISTURE) {
            level.setBlock(pos, state.setValue(MOISTURE, MAX_MOISTURE), 2);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!level.isClientSide
                && CommonHooks.onFarmlandTrample(level, pos, ModBlocks.GOLDEN_DIRT.get().defaultBlockState(),
                        fallDistance, entity)) {
            turnToGoldenDirt(entity, state, level, pos);
        }
        entity.causeFallDamage(fallDistance, 1.0F, entity.damageSources().fall());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Items.GOLDEN_CARROT)) {
            BlockPos cropPos = pos.above();
            BlockState cropState = ModBlocks.GOLDEN_CARROT_CROP.get().defaultBlockState();
            if (level.getBlockState(cropPos).isAir() && cropState.canSurvive(level, cropPos)) {
                if (!level.isClientSide) {
                    level.setBlockAndUpdate(cropPos, cropState);
                    level.gameEvent(GameEvent.BLOCK_PLACE, cropPos, GameEvent.Context.of(player, cropState));
                    level.playSound(null, cropPos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                    stack.consume(1, player);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    private static boolean shouldMaintainFarmland(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(BlockTags.MAINTAINS_FARMLAND);
    }

    private static boolean isNearWater(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        for (BlockPos waterPos : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
            if (state.canBeHydrated(level, pos, level.getFluidState(waterPos), waterPos)) {
                return true;
            }
        }
        return FarmlandWaterManager.hasBlockWaterTicket(level, pos);
    }

    public static void turnToGoldenDirt(@Nullable Entity entity, BlockState state, Level level, BlockPos pos) {
        BlockState replacement = pushEntitiesUp(state, ModBlocks.GOLDEN_DIRT.get().defaultBlockState(), level, pos);
        level.setBlockAndUpdate(pos, replacement);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, replacement));
    }
}