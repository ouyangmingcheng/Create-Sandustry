package com.minechai.create_sandustry.content.mooncrystal;

import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.minechai.create_sandustry.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Shared condition and result logic for the waterlogged Depot and Chute conversion. */
public final class MoonCrystalConversion {
    public static final int START_TIME = 17500;
    public static final int END_TIME = 18500;

    public static boolean canConvert(Level level, BlockPos pos, BlockState state, ItemStack stack) {
        if (!(level instanceof ServerLevel)) {
            return false;
        }
        if (stack.isEmpty() || !stack.is(Items.PRISMARINE_SHARD)) {
            return false;
        }
        if (!state.hasProperty(BlockStateProperties.WATERLOGGED)
                || !state.getValue(BlockStateProperties.WATERLOGGED)) {
            return false;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return false;
        }

        long timeOfDay = Math.floorMod(level.getDayTime(), 24000L);
        if (timeOfDay < START_TIME || timeOfDay >= END_TIME) {
            return false;
        }
        if (level.isRaining() || level.isThundering()) {
            return false;
        }
        return level.canSeeSky(pos.above());
    }

    public static ItemStack rollResult(Level level, ItemStack prismarine) {
        double chance = ModConfigs.MOONCRYSTAL_CONVERSION_CHANCE.get();
        if (level.random.nextDouble() < chance) {
            return new ItemStack(ModItems.MOONCRYSTAL.get());
        }
        return prismarine.copyWithCount(1);
    }

    private MoonCrystalConversion() {
    }
}
