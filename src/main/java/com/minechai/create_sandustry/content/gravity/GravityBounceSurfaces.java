package com.minechai.create_sandustry.content.gravity;

import com.minechai.create_sandustry.content.moonhop.MoonhopBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Central lookup for activation surfaces. Zero means unsupported; coefficients above one are valid. */
public final class GravityBounceSurfaces {
    private GravityBounceSurfaces() {}

    public static double getBounceCoefficient(Level level, BlockPos pos, BlockState state) {
        return state.is(Blocks.SLIME_BLOCK) || state.getBlock() instanceof MoonhopBlock ? 0.8 : 0.0;
    }
}
