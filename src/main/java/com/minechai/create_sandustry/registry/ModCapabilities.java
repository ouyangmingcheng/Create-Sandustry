package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.content.florinolbattery.FlorinolBatteryEnergyStorage;
import com.minechai.create_sandustry.content.sandcasing.SandCasingEnergyStorage;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 重力压制器基座：只有水平四面接受 FE 输入。
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.KINETIC_PRESS.get(),
                (blockEntity, side) -> side != null && side.getAxis().isHorizontal()
                        ? blockEntity.getEnergyStorage() : null);
        // 重力压制器：顶面输入缓存，其余面只允许提取输出缓存。
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.KINETIC_PRESS.get(),
                (blockEntity, side) -> side == net.minecraft.core.Direction.UP
                        ? blockEntity.getInputHandler()
                        : blockEntity.getOutputHandler());
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
