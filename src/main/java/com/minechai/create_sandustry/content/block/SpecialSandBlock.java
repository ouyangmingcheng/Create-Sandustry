package com.minechai.create_sandustry.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Holder;

public abstract class SpecialSandBlock extends FallingBlock {
    protected SpecialSandBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
        if (!level.isClientSide) {
            level.levelEvent(2001, pos, Block.getId(fallingBlock.getBlockState()));
            if (level instanceof ServerLevel serverLevel) {
                SpecialSandEffects.triggerSpecialSandEffect(
                        serverLevel,
                        pos,
                        fallingBlock.getBlockState(),
                        SpecialSandEffects.Trigger.FALLING_BLOCK_DISCARD,
                        null);
            }
        }
    }

    protected final void triggerPlayerBreak(Level level, BlockPos pos, BlockState state, ItemStack tool) {
        if (!level.isClientSide
                && !hasSilkTouch(level, tool)
                && level instanceof ServerLevel serverLevel) {
            SpecialSandEffects.triggerSpecialSandEffect(
                    serverLevel,
                    pos,
                    state,
                    SpecialSandEffects.Trigger.PLAYER_BREAK,
                    null);
        }
    }

    public static boolean hasSilkTouch(Level level, ItemStack tool) {
        if (tool.isEmpty()) {
            return false;
        }
        Holder<Enchantment> silkTouch = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.SILK_TOUCH);
        return tool.getEnchantmentLevel(silkTouch) > 0;
    }
}
