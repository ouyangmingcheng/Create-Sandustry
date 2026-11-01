package com.minechai.create_sandustry.content.block.machine;

import com.minechai.create_sandustry.content.block.entity.FlorinolBatteryBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** 沙金机壳对外暴露的 FE 代理：把连接器的抽电请求转发给所在多方块的总控。 */
public class SandCasingEnergyStorage implements IEnergyStorage {

    private final Level level;
    private final BlockPos pos;
    private FlorinolBatteryBlockEntity cachedController;

    public SandCasingEnergyStorage(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    private FlorinolBatteryBlockEntity controller() {
        if (cachedController != null && !cachedController.isRemoved())
            return cachedController;
        cachedController = FlorinolBatteryBlockEntity.findController(level, pos);
        return cachedController;
    }

    @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        FlorinolBatteryBlockEntity be = controller();
        return be == null ? 0 : be.extractEnergy(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        FlorinolBatteryBlockEntity be = controller();
        return be == null ? 0 : be.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        FlorinolBatteryBlockEntity be = controller();
        return be == null ? 0 : be.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        FlorinolBatteryBlockEntity be = controller();
        return be != null && be.isAssembled() && be.getEnergyStored() > 0;
    }

    @Override public boolean canReceive() { return false; }
}
