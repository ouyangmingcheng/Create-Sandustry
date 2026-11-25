package com.minechai.create_sandustry.content.kineticpress;

import net.neoforged.neoforge.energy.IEnergyStorage;

/** FE capability view of a Kinetic Press base. Input only, exposed from all six sides. */
public final class KineticPressEnergyStorage implements IEnergyStorage {
    private final KineticPressBlockEntity blockEntity;

    public KineticPressEnergyStorage(KineticPressBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return blockEntity.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return blockEntity.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return KineticPressBlockEntity.MAX_ENERGY;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return blockEntity.canReceiveEnergy();
    }
}