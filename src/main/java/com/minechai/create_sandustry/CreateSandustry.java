package com.minechai.create_sandustry;

import com.minechai.create_sandustry.infrastructure.ModBootstrap;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CreateSandustry.MODID)
public class CreateSandustry {
    public static final String MODID = "createsandustry";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateSandustry(IEventBus modEventBus) {
        ModBootstrap.register(modEventBus);
    }
}
