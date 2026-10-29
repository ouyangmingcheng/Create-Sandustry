package com.minechai.create_sandustry.content.event;

import java.util.Set;
import java.util.stream.Collectors;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.block.SandMasterSwordPedestalBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.world.poi.ExtendPoiTypesEvent;

/** 剑座避雷针特性与原版雷击回调桥接。 */
public final class SandMasterSwordPedestalEvents {
    private SandMasterSwordPedestalEvents() {
    }

    /**
     * 只把 {@code has_sand_sword=true} 的剑座状态加入原版避雷针 POI。
     * 自然雷会通过原版 {@code ServerLevel#findLightningTargetAround} 主动寻找它。
     */
    public static void extendPoiTypes(ExtendPoiTypesEvent event) {
        Set<BlockState> chargedPedestalStates = ModBlocks.SAND_MASTER_SWORD_PEDESTAL.get()
                .getStateDefinition()
                .getPossibleStates()
                .stream()
                .filter(state -> state.getValue(SandMasterSwordPedestalBlock.HAS_SAND_SWORD))
                .collect(Collectors.toUnmodifiableSet());
        event.addStatesToPoi(PoiTypes.LIGHTNING_ROD, chargedPedestalStates);
    }

    /** 由 LightningBoltMixin 在原版 {@code powerLightningRod()} 阶段调用。 */
    public static void handleLightningStrike(Level level, BlockPos strikePos) {
        if (level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(strikePos);
        if (state.getBlock() instanceof SandMasterSwordPedestalBlock pedestalBlock) {
            pedestalBlock.onLightningStrike(state, level, strikePos);
        }
    }
}
