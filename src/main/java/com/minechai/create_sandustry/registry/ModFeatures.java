package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.worldgen.geology.GeologyFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 世界生成 Feature 注册。ConfiguredFeature 和 PlacedFeature 由数据包 JSON 注册。
 */
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, CreateSandustry.MODID);

    public static final DeferredHolder<Feature<?>, GeologyFeature> GEOLOGY =
            FEATURES.register("geology", GeologyFeature::new);

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }

    private ModFeatures() {
    }
}
