package com.minechai.create_sandustry.content.florinolsoil;

import java.util.Optional;

import com.minechai.create_sandustry.content.thermalbuffer.ThermalBufferBlockEntity;
import com.minechai.create_sandustry.content.thermalbuffer.ThermalBufferBlock;
import com.minechai.create_sandustry.content.florinolsoil.FlorinolSoilConversionManager;
import com.minechai.create_sandustry.registry.ModTags;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.LitBlazeBurnerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class FlorinolSoilBlock extends Block {
    public static final MapCodec<FlorinolSoilBlock> CODEC = simpleCodec(FlorinolSoilBlock::new);

    public FlorinolSoilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        scheduleIfHeated(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        scheduleIfHeated(level, pos);
    }

    private static void scheduleIfHeated(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || !hasAdjacentHeatSource(serverLevel, pos)) {
            return;
        }
        FlorinolSoilConversionManager.addTicker(serverLevel, pos, true, true);
    }

    public static boolean hasAdjacentHeatSource(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof ThermalBufferBlock) {
                // 热能缓冲器使用 BlockEntity 中的真实热等级，并尊重红石封闭状态。
                if (level.getBlockEntity(neighborPos) instanceof ThermalBufferBlockEntity buffer
                        && !neighborState.getValue(ThermalBufferBlock.POWERED)
                        && buffer.getThermalLevel() >= 1) {
                    return true;
                }
                continue;
            }
            if (isHeatSource(neighborState)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHeatSource(BlockState state) {
        if (state.getBlock() instanceof ThermalBufferBlock) {
            // 红石封闭时即使方块状态里的 HEAT_LEVEL 是旧值也不供热。
            return !state.getValue(ThermalBufferBlock.POWERED)
                    && state.getValue(ThermalBufferBlock.HEAT_LEVEL)
                            .isAtLeast(BlazeBurnerBlock.HeatLevel.SMOULDERING);
        }
        if (state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)) {
            return state.getValue(BlazeBurnerBlock.HEAT_LEVEL)
                    .isAtLeast(BlazeBurnerBlock.HeatLevel.SMOULDERING);
        }
        if (state.getBlock() instanceof LitBlazeBurnerBlock) {
            return true;
        }

        Optional<Boolean> lit = state.getOptionalValue(BlockStateProperties.LIT);
        if (lit.isPresent() && !lit.get()) {
            return false;
        }
        return state.is(ModTags.BlockTags.FLORINOL_SOIL_CATALYZER);
    }
}