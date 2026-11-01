package com.minechai.create_sandustry.content.block.crop;

import com.minechai.create_sandustry.content.block.GoldenFarmlandBlock;
import com.minechai.create_sandustry.registry.ModItems;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class AmethystFlowerBlock extends CropBlock {
    public static final MapCodec<AmethystFlowerBlock> CODEC = simpleCodec(AmethystFlowerBlock::new);
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    public AmethystFlowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return CODEC;
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.AMETHYST_FLOWER_SEEDS.get();
    }

    /** 紫金花只能种植在黄金耕地上。 */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof GoldenFarmlandBlock;
    }

    @Override
    protected int getBonemealAgeIncrease(Level level) {
        return 1;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
