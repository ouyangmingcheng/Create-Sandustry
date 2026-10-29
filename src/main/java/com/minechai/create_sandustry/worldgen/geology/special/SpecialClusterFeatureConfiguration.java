package com.minechai.create_sandustry.worldgen.geology.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * 特殊团簇 Feature 的数据包配置。运行参数存放在独立 TOML 中。
 */
public record SpecialClusterFeatureConfiguration(int schemaVersion) implements FeatureConfiguration {
    public static final Codec<SpecialClusterFeatureConfiguration> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(Codec.INT.fieldOf("schema_version").forGetter(SpecialClusterFeatureConfiguration::schemaVersion))
                    .apply(instance, SpecialClusterFeatureConfiguration::new));
}
