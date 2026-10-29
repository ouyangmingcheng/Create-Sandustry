package com.minechai.create_sandustry.content.block;

import com.minechai.create_sandustry.content.block.entity.LivingSandBlockEntity;
import com.minechai.create_sandustry.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 以方块形式放置的活沙：携带一个 {@link LivingSandBlockEntity}，
 * 在玩家靠近后经过一段时间逐渐“复活”为活沙实体。
 */
public class LivingSandBlock extends BaseEntityBlock {
    public static final MapCodec<LivingSandBlock> CODEC = simpleCodec(LivingSandBlock::new);

    public LivingSandBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LivingSandBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.LIVING_SAND.get(), LivingSandBlockEntity::serverTick);
    }
}
