package com.minechai.create_sandustry.api.florinol;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Last synchronized FE snapshot shown by a bound tool's tooltip and bar. */
public record FlorinolToolEnergySnapshot(int stored, int capacity, boolean available) {
    public static final Codec<FlorinolToolEnergySnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("stored").forGetter(FlorinolToolEnergySnapshot::stored),
            Codec.INT.fieldOf("capacity").forGetter(FlorinolToolEnergySnapshot::capacity),
            Codec.BOOL.fieldOf("available").forGetter(FlorinolToolEnergySnapshot::available)
    ).apply(instance, FlorinolToolEnergySnapshot::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FlorinolToolEnergySnapshot> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, FlorinolToolEnergySnapshot::stored,
                    ByteBufCodecs.VAR_INT, FlorinolToolEnergySnapshot::capacity,
                    ByteBufCodecs.BOOL, FlorinolToolEnergySnapshot::available,
                    FlorinolToolEnergySnapshot::new);

    public float ratio() {
        return available && capacity > 0
                ? Math.max(0.0F, Math.min(1.0F, stored / (float) capacity))
                : 0.0F;
    }
}