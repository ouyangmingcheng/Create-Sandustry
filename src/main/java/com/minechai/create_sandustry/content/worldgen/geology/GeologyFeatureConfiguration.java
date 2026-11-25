package com.minechai.create_sandustry.content.worldgen.geology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * 地质 Feature 的数据包配置。规则参数保存在独立 TOML 中，此对象只负责数据包模式版本。
 */
public record GeologyFeatureConfiguration(int schemaVersion) implements FeatureConfiguration {
    public static final Codec<GeologyFeatureConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("schema_version").forGetter(GeologyFeatureConfiguration::schemaVersion))
            .apply(instance, GeologyFeatureConfiguration::new));
}
