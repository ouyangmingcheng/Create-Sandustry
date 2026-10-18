package com.minechai.create_candustry.registry;

import com.minechai.create_candustry.block.entity.FlorinolBatteryBlockEntity;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        FlorinolBatteryBlockEntity.registerCapabilities(event);
    }
}
