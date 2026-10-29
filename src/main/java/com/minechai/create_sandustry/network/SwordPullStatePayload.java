package com.minechai.create_sandustry.network;

import com.minechai.create_sandustry.CreateSandustry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 服务端 → 客户端：开始或结束对某个无主剑座的右键长按跟踪。 */
public record SwordPullStatePayload(BlockPos pos, boolean active) implements CustomPacketPayload {
    public static final Type<SwordPullStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "sword_pull_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwordPullStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SwordPullStatePayload::pos,
                    ByteBufCodecs.BOOL, SwordPullStatePayload::active,
                    SwordPullStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
