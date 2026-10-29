package com.minechai.create_sandustry.network;

import com.minechai.create_sandustry.CreateSandustry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：玩家松开右键或已离开剑座，取消拔剑。 */
public record SwordPullCancelPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<SwordPullCancelPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "sword_pull_cancel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwordPullCancelPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, SwordPullCancelPayload::pos,
                    SwordPullCancelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
