package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.worldgen.geology.GeologyFeature;
import com.minechai.create_sandustry.worldgen.geology.special.CatLitterClusterFeature;
import com.minechai.create_sandustry.worldgen.geology.special.LivingSandClusterFeature;
import com.minechai.create_sandustry.worldgen.geology.special.SuspiciousGravelClusterFeature;
import com.minechai.create_sandustry.worldgen.geology.special.SuspiciousSandClusterFeature;
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

    public static final DeferredHolder<Feature<?>, SuspiciousSandClusterFeature> SUSPICIOUS_SAND_CLUSTERS =
            FEATURES.register("suspicious_sand_clusters", SuspiciousSandClusterFeature::new);

    public static final DeferredHolder<Feature<?>, SuspiciousGravelClusterFeature> SUSPICIOUS_GRAVEL_CLUSTERS =
            FEATURES.register("suspicious_gravel_clusters", SuspiciousGravelClusterFeature::new);

    public static final DeferredHolder<Feature<?>, CatLitterClusterFeature> CAT_LITTER_CLUSTERS =
            FEATURES.register("cat_litter_clusters", CatLitterClusterFeature::new);

    public static final DeferredHolder<Feature<?>, LivingSandClusterFeature> LIVING_SAND_CLUSTERS =
            FEATURES.register("living_sand_clusters", LivingSandClusterFeature::new);

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }

    private ModFeatures() {
    }
}
