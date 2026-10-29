package com.minechai.create_sandustry.network;

import com.minechai.create_sandustry.CreateSandustry;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 服务端 → 客户端：拔剑扣除一颗沙心时播放不扣血的受伤反馈。 */
public record SwordPullHurtPayload() implements CustomPacketPayload {
    public static final Type<SwordPullHurtPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "sword_pull_hurt"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwordPullHurtPayload> STREAM_CODEC =
            StreamCodec.unit(new SwordPullHurtPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
