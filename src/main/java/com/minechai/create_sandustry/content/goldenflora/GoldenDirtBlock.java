package com.minechai.create_sandustry.content.goldenflora;

import com.minechai.create_sandustry.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/** 黄金泥土：可用锄头耕成黄金耕地。 */
public final class GoldenDirtBlock extends Block {
    public GoldenDirtBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility,
            boolean simulate) {
        if (itemAbility == ItemAbilities.HOE_TILL
                && context.getClickedFace() != Direction.DOWN
                && context.getLevel().getBlockState(context.getClickedPos().above()).isAir()) {
            return ModBlocks.GOLDEN_FARMLAND.get().defaultBlockState();
        }
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }
}