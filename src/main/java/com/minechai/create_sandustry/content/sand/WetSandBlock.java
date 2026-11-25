package com.minechai.create_sandustry.content.sand;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriState;

/**
 * 湿沙子：保留原版沙子作为种植介质时的甘蔗与仙人掌判定。
 *
 * <p>原版甘蔗要求下方为沙类或土类且旁边有水；仙人掌要求下方为沙子或仙人掌且上方不是液体。
 * 湿沙子不是原版 {@code #minecraft:sand} 标签成员，因此通过 NeoForge 的
 * {@link #canSustainPlant} 钩子精确放行这两种植物，而不改变龟蛋、骆驼脚步等其它沙子标签行为。</p>
 */
public class WetSandBlock extends Block {
    public static final MapCodec<WetSandBlock> CODEC = simpleCodec(WetSandBlock::new);

    public WetSandBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition,
                                    Direction facing, BlockState plant) {
        if (facing != Direction.UP) {
            return TriState.DEFAULT;
        }

        if (plant.is(Blocks.SUGAR_CANE)) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos waterPos = soilPosition.relative(direction);
                if (plant.canBeHydrated(level, soilPosition.above(),
                        level.getFluidState(waterPos), waterPos)
                        || level.getBlockState(waterPos).is(Blocks.FROSTED_ICE)) {
                    return TriState.TRUE;
                }
            }
            return TriState.DEFAULT;
        }

        if (plant.is(Blocks.CACTUS)) {
            return level.getBlockState(soilPosition.above().above()).liquid()
                    ? TriState.DEFAULT
                    : TriState.TRUE;
        }

        return TriState.DEFAULT;
    }
}
