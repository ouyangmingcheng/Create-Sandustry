package com.minechai.create_candustry.block;

import com.minechai.create_candustry.config.ModConfigs;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class ThunderSandBlock extends SpecialSandBlock {
    public static final MapCodec<ThunderSandBlock> CODEC = simpleCodec(ThunderSandBlock::new);

    private static final int SAND_DUST_COLOR = 0xDBD3A0;

    public ThunderSandBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return SAND_DUST_COLOR;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                       boolean willHarvest, FluidState fluid) {
        boolean destroyed = super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        if (destroyed) {
            triggerPlayerBreak(level, pos, state, player.getMainHandItem());
        }
        return destroyed;
    }

    public static void handleTrigger(ServerLevel level, BlockPos pos, SpecialSandEffects.Trigger trigger) {
        if (trigger == SpecialSandEffects.Trigger.CAULDRON_CONVERSION) {
            ThunderSandEffects.triggerSingleStrike(level, pos);
            return;
        }

        if (trigger != SpecialSandEffects.Trigger.CHAIN_REACTION
                && level.random.nextFloat() >= ModConfigs.THUNDER_SAND_TRIGGER_CHANCE.get().floatValue()) {
            return;
        }

        int minStrikes = ModConfigs.THUNDER_SAND_MIN_STRIKES.get();
        int maxStrikes = ModConfigs.THUNDER_SAND_MAX_STRIKES.get();
        int strikeCount = minStrikes + level.random.nextInt(Math.max(1, maxStrikes - minStrikes + 1));
        ThunderSandEffects.scheduleStrikes(level, Vec3.atCenterOf(pos), strikeCount);
    }
}