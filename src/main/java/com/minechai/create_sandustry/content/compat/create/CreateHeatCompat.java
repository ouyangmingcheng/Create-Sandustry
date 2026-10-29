package com.minechai.create_sandustry.content.compat.create;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.block.machine.ThermalBufferBlock;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

/**
 * Create 锅炉热源适配。
 *
 * <p>锅炉使用 {@link BoilerHeater} 的数值热等级，而不是工作盆读取的
 * {@code HEAT_LEVEL} 属性，因此热能缓冲器需要单独注册。</p>
 */
public final class CreateHeatCompat {
    public static void registerBoilerHeater() {
        BoilerHeater.REGISTRY.register(ModBlocks.THERMAL_RELAY.get(), (level, pos, state) -> {
            if (state.getValue(ThermalBufferBlock.POWERED)) {
                return BoilerHeater.NO_HEAT;
            }

            HeatLevel heatLevel = state.getValue(ThermalBufferBlock.HEAT_LEVEL);
            return switch (heatLevel) {
                case SMOULDERING -> BoilerHeater.PASSIVE_HEAT;
                case FADING, KINDLED -> 1;
                case SEETHING -> 2;
                default -> BoilerHeater.NO_HEAT;
            };
        });
    }

    private CreateHeatCompat() {
    }
}
