package com.minechai.create_sandustry.api.florinol;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** A persistent reference from a tool item to one assembled Florinol battery controller. */
public record FlorinolToolBinding(ResourceLocation dimension, BlockPos pos, UUID batteryId) {
    public static final Codec<FlorinolToolBinding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(FlorinolToolBinding::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(FlorinolToolBinding::pos),
            UUIDUtil.CODEC.fieldOf("battery_id").forGetter(FlorinolToolBinding::batteryId)
    ).apply(instance, FlorinolToolBinding::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FlorinolToolBinding> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, FlorinolToolBinding::dimension,
                    BlockPos.STREAM_CODEC, FlorinolToolBinding::pos,
                    UUIDUtil.STREAM_CODEC, FlorinolToolBinding::batteryId,
                    FlorinolToolBinding::new);

    public ResourceKey<Level> dimensionKey() {
        return ResourceKey.create(Registries.DIMENSION, dimension);
    }
}