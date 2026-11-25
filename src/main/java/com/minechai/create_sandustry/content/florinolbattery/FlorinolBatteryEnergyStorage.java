package com.minechai.create_sandustry.content.florinolbattery;

import com.minechai.create_sandustry.content.florinolbattery.FlorinolBatteryBlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** 弗洛林电池对外暴露的 FE 储能（只出不进），供 createaddition:connector 等自动连接。 */
public class FlorinolBatteryEnergyStorage implements IEnergyStorage {

    private final FlorinolBatteryBlockEntity blockEntity;

    public FlorinolBatteryEnergyStorage(FlorinolBatteryBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return blockEntity.extractEnergy(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return blockEntity.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return blockEntity.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return blockEntity.isAssembled() && blockEntity.getEnergyStored() > 0;
    }

    @Override
    public boolean canReceive() {
        return false;
    }
}