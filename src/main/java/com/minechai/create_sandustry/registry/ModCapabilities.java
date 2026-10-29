package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.content.block.machine.FlorinolBatteryEnergyStorage;
import com.minechai.create_sandustry.content.block.machine.SandCasingEnergyStorage;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 总控方块直接暴露 FE。
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.FLORINOL_BATTERY.get(),
                (blockEntity, side) -> new FlorinolBatteryEnergyStorage(blockEntity));

        // 沙金机壳（骨架）同样暴露 FE，把连接器的抽电请求转发给所在多方块的总控。
        event.registerBlock(
                Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, blockEntity, side) -> new SandCasingEnergyStorage(level, pos),
                ModBlocks.SAND_CASING.get());
    }
}
